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
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.api.protocol.Protocol;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.ModelCloner;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.kevoree.pmodeling.api.trace.TraceSequence;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.undertow.Handlers.websocket;
import static org.kevoree.api.protocol.Protocol.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@GroupType
public class WSGroup implements ModelListener, Runnable {

    private AtomicBoolean diverge = new AtomicBoolean(false);

    @KevoreeInject
    public Context context;

    @KevoreeInject
    public ModelService modelService;

    @KevoreeInject
    private KevScriptService kevsService;

    @Param(optional = true, fragmentDependent = true, defaultValue = "9000")
    Integer port;

    public void setMaster(String master) {
        this.master = master;
    }

    @Param(optional = true)
    String master;

    @Param(defaultValue = "")
    private String onConnect = "";

    @Param(defaultValue = "")
    private String onDisconnect = "";


    public void setPort(Integer port) throws IOException, InterruptedException {
        this.port = port;
        if (running) {
            serverHandler.stop();
            serverHandler = null;
            serverHandler = Undertow.builder()
                    .addHttpListener(port, "0.0.0.0")
                    .setHandler(websocket(new InternalWebSocketServer()))
                    .build();
            modelService.registerModelListener(this);
            serverHandler.start();
        }
    }

    private boolean running = false;
    private ScheduledExecutorService scheduledThreadPool;
    private Undertow serverHandler;
    private KevoreeFactory factory = new DefaultKevoreeFactory();
    private JSONModelSerializer serializer = factory.createJSONSerializer();
    private ModelCloner cloner = factory.createModelCloner();


    private class InternalWebSocketServer implements WebSocketConnectionCallback {

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            channel.getReceiveSetter().set(new AbstractReceiveListener() {

                @Override
                protected void onFullTextMessage(WebSocketChannel webSocket, BufferedTextMessage message) {
                    String msg = message.getData();
                    try {
                        Message parsedMsg = Protocol.parse(msg);
                        if (parsedMsg == null) {
                            Log.warn(WSGroup.this.getClass().getSimpleName() + "  \"{}\" received an unknown message '{}'", context.getInstanceName(), msg);
                        } else {
                            switch (parsedMsg.getType()) {
                                case REGISTER_TYPE:
                                    RegisterMessage rm = (RegisterMessage) parsedMsg;
                                    cache.put(rm.getNodeName(), webSocket);
                                    rcache.put(webSocket, rm.getNodeName());
                                    if (isMaster()) {
                                        Log.info("New client registered \"{}\"", rm.getNodeName());
                                        ContainerRoot modelToApply = cloner.clone(modelService.getCurrentModel().getModel());
                                        if (rm.getModel() != null && !rm.getModel().equals("null")) {
                                            // new registered model has a model to share: merging it locally
                                            ContainerRoot recModel = (ContainerRoot) jsonModelLoader.loadModelFromString(rm.getModel()).get(0);
                                            TraceSequence tseq = compare.merge(modelToApply, recModel);
                                            Log.info("Merging his model with mine...", ((RegisterMessage) parsedMsg).getNodeName());
                                            tseq.applyOn(modelToApply);
                                        }
                                        // add onConnect logic
                                        try {
                                            kevsService.execute(tpl(onConnect, rm.getNodeName()), modelToApply);
                                        } catch (Exception e) {
                                            Log.error("Unable to parse onConnect KevScript. Broadcasting model without onConnect process.");
                                        } finally {
                                            String recModelStr = serializer.serialize(modelToApply);
                                            PushMessage pushMessage = new PushMessage(recModelStr);

                                            // update locally
                                            modelService.update(modelToApply, applied -> Log.info("Merge model result: {}", applied));

                                            // broadcast changes
                                            Log.info("Broadcasting merged model to all connected clients");
                                            for (WebSocketChannel client : cache.values()) {
                                                if (client.isOpen()) {
                                                    WebSockets.sendText(pushMessage.toRaw(), client, null);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case PULL_TYPE:
                                    String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                                    Log.info("{} \"{}\": pull requested", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                    WebSockets.sendText(modelReturn, webSocket, null);
                                    break;
                                case PUSH_TYPE:
                                    PushMessage pm = (PushMessage) parsedMsg;
                                    try {
                                        Log.info("{} \"{}\": push received, applying locally...", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                        ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                                        if (hasMaster()) {
                                            if (isMaster()) {
                                                int count = 0;
                                                for (WebSocketChannel ws : cache.values()) {
                                                    count++;
                                                    if (ws.isOpen()) {
                                                        WebSockets.sendText(pm.toRaw(), ws, null);
                                                    }
                                                }

                                                if (count > 0) {
                                                    Log.info("Broadcast model over {} client{}", count, (count > 1) ? "s" : "");
                                                }
                                            }
                                        } else {
                                            Log.info("No master specified, model will NOT be send to all other nodes");
                                        }

                                        modelService.update(model, new UpdateCallback() {
                                            @Override
                                            public void run(Boolean applied) {
                                                Log.info("{} \"{}\" update result: {}", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), applied);
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.warn("{} \"{}\" received a malformed push message '{}'", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), msg);
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
                    try {
                        modelService.submitScript(tpl(onDisconnect, nodeName), new UpdateCallback() {
                            @Override
                            public void run(Boolean applied) {
                                Log.info("{} \"{}\" onDisconnect result from {}: {}", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), nodeName, applied);
                            }
                        });
                    } catch (Exception e) {
                        Log.error("Unable to parse onDisconnect KevScript. No changes made after the disconnection of "+nodeName);
                    }
                    cache.remove(nodeName);
                }
                rcache.remove(ws);
            });
        }



        private String tpl(String tpl, String nodeName) {
            return tpl
                    .replaceAll("\\{nodeName\\}", nodeName)
                    .replaceAll("\\{groupName\\}", context.getInstanceName());
        }
    }

    @Start
    public void startWSGroup() {
        if (this.hasMaster()) {
            if (this.isMaster()) {
                serverHandler = Undertow.builder()
                        .addHttpListener(port, "0.0.0.0")
                        .setHandler(websocket(new InternalWebSocketServer()))
                        .build();
                serverHandler.start();
                Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" listen on {}", context.getInstanceName(), port);
            } else {
                modelService.registerModelListener(this);
                running = true;
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
        running = false;

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

    @Override
    public boolean initUpdate(UpdateContext context) {
        return isMaster() || context.getCallerPath().equals(this.context.getPath()) || !pushToMaster(context.getProposedModel());

    }

    public boolean pushToMaster(ContainerRoot model) {
        if (masterClient != null && masterClient.isOpen()) {
            PushMessage pushMessage = new PushMessage(jsonModelSaver.serialize(model));
            WebSockets.sendText(pushMessage.toRaw(), masterClient, null);
            return true;
        } else {
            diverge.set(true);
            Log.warn("Could not join master node : {}, diverge locally", master);
            return false;
        }
    }

    @Override
    public boolean afterLocalUpdate(UpdateContext context) {
        if (isMaster()) {
            for (WebSocketChannel wsClient : cache.values()) {
                if (wsClient.isOpen()) {
                    String currentModel = jsonModelSaver.serialize(context.getProposedModel());
                    PushMessage pushMessage = new PushMessage(currentModel);
                    WebSockets.sendText(pushMessage.toRaw(), wsClient, null);
                    Log.info("Forward to {}", rcache.get(wsClient));
                } else {
                    Log.error("Disconnected Client " + rcache.get(wsClient));
                }
            }
        }
        return true;
    }

    private WebSocketChannel masterClient;

    public void run() {
        try {
            if (!isMaster() && context != null) {
                if (masterClient == null || !masterClient.isOpen()) {
                    ContainerRoot lastModel = modelService.getCurrentModel().getModel();
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
                                masterClient = createWSClient(ip, port, context.getNodeName(), modelService, diverge);
                                if (masterClient != null && masterClient.isOpen()) {
                                    Log.info("Master connection opened on {}:{}", ip, port);
                                    return;
                                }
                            }
                            masterClient = createWSClient(defaultIP, port, context.getNodeName(), modelService, diverge);
                            if (masterClient != null && masterClient.isOpen()) {
                                Log.info("Master connection opened on {}:{}", defaultIP, port);
                            }
                        } else {
                            Log.info("Master node '{}' is not defined in the model. You must add it.", master);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Error while connecting to master server (is server reachable ?)");
        }
    }

    public WebSocketChannel createWSClient(final String ip, final String port, String currentNodeName, final ModelService modelService, final AtomicBoolean diverge) throws IOException {
        Xnio xnio = Xnio.getInstance(io.undertow.websockets.client.WebSocketClient.class.getClassLoader());
        XnioWorker worker = xnio.createWorker(OptionMap.builder()
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
                        case PUSH_TYPE:
                            //push from master
                            diverge.set(false);
                            PushMessage pm = (PushMessage) parsedMsg;
                            ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                            modelService.update(model, new UpdateCallback() {
                                @Override
                                public void run(Boolean applied) {
                                    Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" update result: {}", context.getInstanceName(), applied);
                                }
                            });
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

        // sending current model
        String currentModel = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
        WebSockets.sendText(new RegisterMessage(currentNodeName, currentModel).toRaw(), client[0], null);
        return client[0];
    }

    @Override
    public boolean preUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public void modelUpdated() {}

    @Override
    public void preRollback(UpdateContext context) {}

    @Override
    public void postRollback(UpdateContext context) {}
}
