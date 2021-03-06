package org.kevoree.library;

import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.kevoree.*;
import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.service.KevScriptService;
import org.kevoree.service.ModelService;
import org.kevoree.api.handler.AbstractModelListener;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.protocol.Protocol.Message;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.json.JSONModelLoader;
import org.kevoree.modeling.api.json.JSONModelSerializer;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static io.undertow.Handlers.websocket;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@GroupType(version=1, description = "This group uses <strong>WebSockets</strong> to propagate models over the connected nodes."+
        "<br/>If the attribute <strong>master</strong> is specified (using the instance "+
        "name of one of the connected nodes) then a WebSocket server will be listening "+
        "on that node using the <strong>port</strong> attribute specified in the fragment "+
        "dictionary of that particular node and every other nodes connected to that group "+
        "will try to connect to that <strong>master</strong> node."+
        "</br>If <strong>master</strong> is empty, then every connected node will try to "+
        "start a WebSocket server using their <strong>port</strong> fragment attribute."+
        "<br/><br/>The attributes <strong>onConnect</strong> and <strong>onDisconnect</strong> "+
        "expects KevScript strings to be given to them optionally. If set, "+
        "<strong>onConnect</strong> KevScript will be executed on the <strong>master</strong> node "+
        "when a new client connects to the master server (and <strong>onDisconnect</strong> "+
        "will be executed when a node disconnects from the master server)"+
        "<br/><br/><em>NB: onConnect & onDisconnect can reference the current node that "+
        "triggered the process by using this notation: {nodeName}</em>"+
        "<br/><em>NB2: {groupName} is also available and resolves to the current WSGroup instance name</em>"+
        "<br/><em>NB3: onConnect & onDisconnect are not triggered if the client nodeName does not match the regex given in the <strong>filter</strong> parameter</em>")
public class WSGroup extends AbstractModelListener implements Runnable {

    private AtomicBoolean lock = new AtomicBoolean(false);

    @KevoreeInject
    public Context context;

    @KevoreeInject
    public ModelService modelService;

    @KevoreeInject
    private KevScriptService kevsService;

    @Param(optional = true, fragmentDependent = true)
    private int port = 9000;

    @Param(optional = true)
    private String master;

    @Param(optional = true)
    private String filter;

    @Param
    private String onConnect = "";

    @Param
    private String onDisconnect = "";

    private String currentMaster;
    private int currentPort;
    private ScheduledExecutorService scheduledThreadPool;
    private Undertow serverHandler;
    private KevoreeFactory factory = new DefaultKevoreeFactory();
    private JSONModelSerializer serializer = factory.createJSONSerializer();
    private ModelCloner cloner = factory.createModelCloner();

    private class InternalWebSocketServer implements WebSocketConnectionCallback {

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

            allConnectedClients.add(channel);
            channel.getReceiveSetter().set(new AbstractReceiveListener() {

                @Override
                protected void onFullTextMessage(WebSocketChannel webSocket, BufferedTextMessage message) {
                    final String msg = message.getData();
                    try {
                        final Message parsedMsg = Protocol.parse(msg);
                        if (parsedMsg == null) {
                            Log.warn(WSGroup.this.getClass().getSimpleName() + "  \"{}\" received an unknown message '{}'", context.getInstanceName(), msg);
                        } else {
                            switch (parsedMsg.getType()) {
                                case Protocol.REGISTER_TYPE:
                                    Protocol.RegisterMessage rm = (Protocol.RegisterMessage) parsedMsg;
                                    cache.put(rm.getNodeName(), webSocket);
                                    rcache.put(webSocket, rm.getNodeName());
                                    if (isMaster()) {
                                        Log.info("New client registered \"{}\"", rm.getNodeName());
                                        ContainerRoot modelToApply = cloner.clone(modelService.getCurrentModel());
                                        if (rm.getModel() != null && !rm.getModel().equals("null")) {
                                            // new registered model has a model to share: merging it locally
                                            ContainerRoot recModel = (ContainerRoot) jsonModelLoader.loadModelFromString(rm.getModel()).get(0);
                                            TraceSequence tseq = compare.merge(recModel, modelToApply);
                                            tseq.applyOn(recModel);
                                            modelToApply = recModel;
                                            Log.debug("Node's \"{}\" model has been merged with the current one", rm.getNodeName());
                                        }
                                        if (checkFilter(rm.getNodeName())) {
                                            // add onConnect logic
                                            try {
                                                Log.debug("onConnect KevScript to process:");
                                                final String onConnectKevs = tpl(onConnect, rm.getNodeName());
                                                if (!onConnectKevs.isEmpty()) {
                                                    Log.debug("===== onConnect KevScript =====");
                                                    Log.debug(onConnectKevs);
                                                    Log.debug("===============================");
                                                    kevsService.execute(onConnectKevs, modelToApply);
                                                } else {
                                                    Log.debug("onConnect KevScript empty");
                                                }
                                            } catch (Exception e) {
                                                Log.error("Unable to parse onConnect KevScript. Broadcasting model without onConnect process.", e);
                                            } finally {
                                                // update locally
                                                modelService.update(modelToApply);
                                            }
                                        } else {
                                            // update locally
                                            Log.debug("Applying merged model locally (no filter)");
                                            modelService.update(modelToApply);
                                        }
                                    }
                                    break;
                                case Protocol.PULL_TYPE:
                                    String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel());
                                    Log.info("{} \"{}\": pull requested", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                    WebSockets.sendText(modelReturn, webSocket, null);
                                    break;
                                case Protocol.PUSH_TYPE:
                                    Protocol.PushMessage pm = (Protocol.PushMessage) parsedMsg;
                                    try {
                                        Log.info("{} \"{}\": push received, applying locally...", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                        ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                                        modelService.update(model);
                                    } catch (Exception e) {
                                        Log.warn("{} \"{}\" received a malformed push message '{}'", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), msg);
                                    }
                                    break;

                                case Protocol.KEVS_TYPE:
                                    Log.info("{} \"{}\": push received (kevscript), applying KevScript locally...",
                                            WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                    ContainerRoot clonedModel = cloner.clone(modelService.getCurrentModel());
                                    try {
                                        kevsService.execute(((Protocol.PushKevSMessage) parsedMsg).getKevScript(), clonedModel);
                                        modelService.update(clonedModel);
                                    } catch (Exception e) {
                                        Log.error("KevScript error", e);
                                    }

                                    break;
                                default:
                                    Log.warn("{} \"{}\" unhandled message '{}'", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), msg);
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                    // Overriding onFullCloseMessage so that onFullTextMessage is called even though the data were sent by fragments
                }

                @Override
                protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                    String name = rcache.get(webSocketChannel);
                    if (name != null) {
                        cache.remove(name);
                    }
                    rcache.remove(webSocketChannel);
                    allConnectedClients.remove(webSocketChannel);
                }

                @Override
                protected void onError(WebSocketChannel webSocket, Throwable error) {
                    Log.error("{} \"{}\": {}", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), error.getMessage());
                    try {
                        if (webSocket != null) {
                            webSocket.close();
                            if (rcache != null) {
                                String name = rcache.get(webSocket);
                                if (name != null) {
                                    cache.remove(name);
                                }
                                rcache.remove(webSocket);
                                allConnectedClients.remove(webSocket);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            channel.resumeReceives();
            channel.addCloseTask(ws -> {
                final String nodeName = rcache.get(ws);
                if (nodeName != null) {
                    Log.info("Client node '{}' disconnected", nodeName);
                    if (checkFilter(nodeName)) {
                        // add onDisconnect logic
                        try {
                            final String onDisconnectKevs = tpl(onDisconnect, nodeName);
                            if (!onDisconnectKevs.isEmpty()) {
                                Log.debug("===== onDisconnect KevScript =====");
                                Log.debug(onDisconnectKevs);
                                Log.debug("==================================");
                                modelService.submitScript(onDisconnectKevs,
                                        applied -> Log.info("{} \"{}\" onDisconnect result from {}: {}",
                                                WSGroup.this.getClass().getSimpleName(), context.getInstanceName(),
                                                nodeName, applied));
                            } else {
                                Log.debug("onDisconnect KevScript empty");
                            }
                        } catch (Exception e) {
                            Log.error("Unable to parse onDisconnect KevScript. No changes made after the disconnection of " + nodeName, e);
                        }
                    }
                    cache.remove(nodeName);
                }
                rcache.remove(ws);
                allConnectedClients.remove(ws);
            });
        }



        private String tpl(String tpl, String nodeName) {
            return tpl
                    .replaceAll("\\{nodeName\\}", nodeName)
                    .replaceAll("\\{groupName\\}", context.getInstanceName())
                    .trim();
        }
    }

    @Start
    public void startWSGroup() {
        this.currentMaster = master;
        this.currentPort = port;
        modelService.registerModelListener(this);
        if (this.hasMaster()) {
            if (this.isMaster()) {
                serverHandler = Undertow.builder()
                        .addHttpListener(port, "0.0.0.0")
                        .setHandler(websocket(new InternalWebSocketServer()))
                        .build();
                serverHandler.start();
                Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" listen on {}", context.getInstanceName(), port);
            } else {
                scheduledThreadPool = Executors.newScheduledThreadPool(1);
                scheduledThreadPool.scheduleAtFixedRate(this, 0, 3000, TimeUnit.MILLISECONDS);
            }
        } else {
            serverHandler = Undertow.builder()
                    .addHttpListener(port, "0.0.0.0")
                    .setHandler(websocket(new InternalWebSocketServer()))
                    .build();
            serverHandler.start();
            Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" listen on {}", context.getInstanceName(), port);
        }
    }

    private Set<WebSocketChannel> allConnectedClients = Collections.synchronizedSet(new HashSet<>());
    private Map<String, WebSocketChannel> cache = new ConcurrentHashMap<String, WebSocketChannel>();
    private Map<WebSocketChannel, String> rcache = new ConcurrentHashMap<WebSocketChannel, String>();

    @Stop
    public void stopWSGroup() throws IOException, InterruptedException {
        if (scheduledThreadPool != null) {
            scheduledThreadPool.shutdownNow();
            modelService.unregisterModelListener(this);
        }
        if (serverHandler != null) {
            serverHandler.stop();
        }
        if (cache != null) {
            cache.clear();
        }
        if (rcache != null) {
            rcache.clear();
        }
        if (masterClient != null && masterClient.isOpen()) {
            masterClient.close();
            masterClient = null;
        }
    }

    @Update
    public void update() throws IOException, InterruptedException {
        if ((this.currentMaster == null && this.master != null)
                || (this.currentMaster != null && this.master == null)
                || (this.currentMaster != null && (!this.currentMaster.equals(this.master) || this.currentPort != this.port))) {
            this.stopWSGroup();
            this.startWSGroup();
        }
    }

    private static JSONModelLoader jsonModelLoader = new JSONModelLoader(new DefaultKevoreeFactory());
    private static JSONModelSerializer jsonModelSaver = new JSONModelSerializer();
    private static ModelCompare compare = new ModelCompare(new DefaultKevoreeFactory());

    private boolean isMaster() {
        return (hasMaster() && master.equals(context.getNodeName()));
    }

    private boolean hasMaster() {
        return (master != null && master.length() > 0);
    }

    private boolean checkFilter(String nodeName) {
        if (this.filter != null && !this.filter.isEmpty()) {
            Pattern pattern = Pattern.compile(this.filter);
            return pattern.matcher(nodeName).matches();
        }
        return true;
    }

    private WebSocketChannel masterClient;

    public void run() {
        try {
            if (!isMaster() && context != null) {
                if (masterClient == null || !masterClient.isOpen()) {
                    ContainerRoot lastModel = modelService.getCurrentModel();
                    Group selfGroup = (Group) lastModel.findByPath(context.getPath());
                    //localize master node
                    if (selfGroup != null && master != null) {
                        FragmentDictionary masterDico = selfGroup.findFragmentDictionaryByID(master);
                        String defaultIP = "127.0.0.1";
                        String port = "9000";
                        if (masterDico != null) {
                            Value val = masterDico.findValuesByID("port");
                            port = val.getValue();
                        }
                        List<String> addresses = new ArrayList<String>();
                        ContainerNode node = lastModel.findNodesByID(master);
                        if (node != null) {
                            for (NetworkInfo net : node.getNetworkInformation()) {
                                for (Value prop : net.getValues()) {
                                    if (net.getName().toLowerCase().contains("ip") || prop.getName().toLowerCase().contains("ip")) {
                                        addresses.add(prop.getValue());
                                    }
                                }
                            }
                            for (String ip : addresses) {
                                masterClient = createWSClient(ip, port, context.getNodeName(), modelService);
                                if (masterClient != null && masterClient.isOpen()) {
                                    Log.info("Master connection opened on {}:{}", ip, port);
                                    String currentModel = jsonModelSaver.serialize(modelService.getCurrentModel());
                                    WebSockets.sendText(new Protocol.RegisterMessage(context.getNodeName(), currentModel).toRaw(), masterClient, null);
                                    return;
                                }
                            }
                            masterClient = createWSClient(defaultIP, port, context.getNodeName(), modelService);
                            if (masterClient != null && masterClient.isOpen()) {
                                Log.info("Master connection opened on {}:{}", defaultIP, port);
                                String currentModel = jsonModelSaver.serialize(modelService.getCurrentModel());
                                WebSockets.sendText(new Protocol.RegisterMessage(context.getNodeName(), currentModel).toRaw(), masterClient, null);
                            }
                        } else {
                            Log.info("Master node '{}' is not defined in the model. You must add it.", master);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.warn("Unable to connect to master server (is server reachable ?)");
        }
    }

    public WebSocketChannel createWSClient(final String ip, final String port, String currentNodeName, final ModelService modelService) throws IOException {
        XnioWorker worker = Xnio.getInstance(Undertow.class.getClassLoader())
                .createWorker(OptionMap.builder()
                        .set(Options.WORKER_IO_THREADS, 2)
                        .set(Options.CONNECTION_HIGH_WATER, 1000000)
                        .set(Options.CONNECTION_LOW_WATER, 1000000)
                        .set(Options.WORKER_TASK_CORE_THREADS, 30)
                        .set(Options.WORKER_TASK_MAX_THREADS, 30)
                        .set(Options.TCP_NODELAY, true)
                        .set(Options.CORK, true)
                        .getMap());
        ByteBufferSlicePool buffer = new ByteBufferSlicePool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 1024, 1024);
        final WebSocketChannel[] client = new WebSocketChannel[1];
        URI uri;
        if (ip.contains(":")) {
            uri = URI.create("ws://[" + ip + "]:" + port);
        } else {
            uri = URI.create("ws://" + ip + ":" + port);
        }

        client[0] = WebSocketClient.connect(worker, buffer, OptionMap.EMPTY, uri, WebSocketVersion.V13).get();
        client[0].getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                String msg = message.getData();
                Protocol.Message parsedMsg = Protocol.parse(msg);
                if (parsedMsg == null) {
                    Log.warn(WSGroup.this.getClass().getSimpleName() + " \"{}\" unknown message '{}'", context.getInstanceName(), msg);
                } else {
                    switch (parsedMsg.getType()) {
                        case Protocol.PUSH_TYPE:
                            try {
                                Protocol.PushMessage pm = (Protocol.PushMessage) parsedMsg;
                                ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                                modelService.update(model);
                            } catch (Exception err) {
                                lock.set(false);
                            }
                            break;
                        default:
                            Log.warn(WSGroup.this.getClass().getSimpleName() + " \"{}\" unhandled message '{}'", context.getInstanceName(), msg);
                            break;
                    }
                }
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
                Log.error("Something went wrong while connecting to {}:{}", ip, port);
            }
        });
        client[0].resumeReceives();
//        // register on master
//        ContainerRoot model = modelService.getCurrentModel().
//        if (model == null) {
//            Log.debug("No pending model...taking current one");
//            model = modelService.getCurrentModel();
//            Log.debug("Found current node in model {}", model.findNodesByID(modelService.getNodeName()).getName());
//        }
//        String currentModel = jsonModelSaver.serialize(model);
//        WebSockets.sendText(new Protocol.RegisterMessage(currentNodeName, currentModel).toRaw(), client[0], null);
        return client[0];
    }

    @Override
    public void updateSuccess(UpdateContext ctx) {
        if (!lock.get()) {
            String modelStr = serializer.serialize(this.modelService.getCurrentModel());
            Protocol.PushMessage pushMessage = new Protocol.PushMessage(modelStr, null);
            if (isMaster()) {
                // broadcast changes
                /*
                 * We create a new collection in order to avoid any
                 * concurrent exception since a client can close its own
                 * connection while we send data to others (synchronized
                 * collections do not allow a concurrent thread to remove
                 * elements during an iteration)
                 */
                if (allConnectedClients.size() > 0) {
                    Log.info("Broadcasting new model to all clients ("+this.allConnectedClients.size()+")");
                }
                for (WebSocketChannel client : new HashSet<>(allConnectedClients)) {
                    if (client != null && client.isOpen()) {
                        WebSockets.sendText(pushMessage.toRaw(), client, null);
                    }
                }
            } else {
                if (this.masterClient != null && this.masterClient.isOpen()) {
                    Log.debug("[{}] Notifying master with local model", context.getNodeName());
                    WebSockets.sendText(pushMessage.toRaw(), this.masterClient, null);
                } else {
                    Log.debug("Unable to notify master server. Not connected to master yet.");
                }
            }
        }
    }
}
