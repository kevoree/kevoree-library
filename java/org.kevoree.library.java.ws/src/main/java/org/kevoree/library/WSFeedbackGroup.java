package org.kevoree.library;

import static io.undertow.Handlers.websocket;
import static org.kevoree.library.protocol.Protocol.PULL_TYPE;
import static org.kevoree.library.protocol.Protocol.PUSH_TYPE;
import static org.kevoree.library.protocol.Protocol.REGISTER_TYPE;
import static org.kevoree.library.protocol.Protocol.RESULT_TYPE;
import static org.kevoree.library.protocol.Protocol.STATUS_TYPE;

import java.io.IOException;
import java.net.URI;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.FragmentDictionary;
import org.kevoree.Group;
import org.kevoree.NetworkInfo;
import org.kevoree.Value;
import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Context;
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.feedback.ResultTaskService;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.protocol.Protocol.Message;
import org.kevoree.library.protocol.Protocol.PushMessage;
import org.kevoree.library.protocol.Protocol.RegisterMessage;
import org.kevoree.library.protocol.Protocol.ResultMessage;
import org.kevoree.library.protocol.Protocol.StatusMessage;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.ModelCloner;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.kevoree.pmodeling.api.trace.TraceSequence;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

/**
 * Created with IntelliJ IDEA. User: duke Date: 29/11/2013 Time: 12:07
 */

@GroupType(version=1, description = "This group uses <strong>WebSockets</strong> to propagate models over the connected nodes."
        + "<br/>If the attribute <strong>master</strong> is specified (using the instance "
        + "name of one of the connected nodes) then a WebSocket server will be listening "
        + "on that node using the <strong>port</strong> attribute specified in the fragment "
        + "dictionary of that particular node and every other nodes connected to that group "
        + "will try to connect to that <strong>master</strong> node."
        + "</br>If <strong>master</strong> is empty, then every connected node will try to "
        + "start a WebSocket server using their <strong>port</strong> fragment attribute."
        + "<br/><br/>The attributes <strong>onConnect</strong> and <strong>onDisconnect</strong> "
        + "expects KevScript strings to be given to them optionally. If set, "
        + "<strong>onConnect</strong> KevScript will be executed on the <strong>master</strong> node "
        + "when a new client connects to the master server (and <strong>onDisconnect</strong> "
        + "will be executed when a node disconnects from the master server)"
        + "<br/><br/><em>NB: onConnect & onDisconnect can reference the current node that "
        + "triggered the process by using this notation: {nodeName}</em>"
        + "<br/><em>NB2: {groupName} is also available and resolves to the current WSGroup instance name</em>"
        + "<br/><em>NB3: onConnect & onDisconnect are not triggered if the client nodeName does not match the regex given in the <strong>filter</strong> parameter</em>")
public class WSFeedbackGroup implements ModelListener, Runnable {

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

    @Param(optional = true)
    String filter;

    @Param(defaultValue = "")
    private String onConnect = "";

    @Param(defaultValue = "")
    private String onDisconnect = "";

    @Param(optional = false, defaultValue = "30000")
    private Long delay;

    @Param(optional = false, defaultValue = "true")
    private Boolean strict;

    private ScheduledExecutorService scheduledThreadPool;
    private Undertow serverHandler;
    private KevoreeFactory factory = new DefaultKevoreeFactory();
    private JSONModelSerializer serializer = factory.createJSONSerializer();
    private ModelCloner cloner = factory.createModelCloner();

    private ResultTaskService resultTaskService;

    private class InternalWebSocketServer implements WebSocketConnectionCallback {

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

            allConnectedClients.add(channel);
            channel.getReceiveSetter().set(new AbstractReceiveListener() {

                @Override
                protected void onFullTextMessage(WebSocketChannel webSocket, BufferedTextMessage message) {
                    String msg = message.getData();
                    try {
                        Message parsedMsg = Protocol.parse(msg);
                        if (parsedMsg == null) {
                            Log.warn(
                                    WSFeedbackGroup.this.getClass().getSimpleName()
                                            + "  \"{}\" received an unknown message '{}'",
                                    context.getInstanceName(), msg);
                        } else {
                            switch (parsedMsg.getType()) {
                            case REGISTER_TYPE:
                                handleRegister(webSocket, parsedMsg);
                                break;
                            case PULL_TYPE:
                                handlePull(webSocket);
                                break;
                            case PUSH_TYPE:
                                handlePush(msg, parsedMsg);
                                break;
                            case RESULT_TYPE:
                                handleResult(parsedMsg);
                                break;
                            case STATUS_TYPE:
                                handleStatus(webSocket, parsedMsg);
                                break;
                            default:
                                Log.warn("{} \"{}\" unhandled message '{}'",
                                        WSFeedbackGroup.this.getClass().getSimpleName(), context.getInstanceName(),
                                        msg);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                private void handleStatus(final WebSocketChannel webSocket, final Message parsedMsg) {
                    final StatusMessage sm = (StatusMessage) parsedMsg;
                    final String uid = sm.getUid();
                    final ResultTaskService.Status status = resultTaskService.getStatus(uid);
                    WebSockets.sendText(String.valueOf(status), webSocket, null);
                }

                private void handleResult(Message parsedMsg) {
                    final ResultMessage resMsg = (ResultMessage) parsedMsg;
                    saveDeploymentResult(resMsg.getNode(), resMsg.getUid(), resMsg.getResult());
                }

                private void handlePull(WebSocketChannel webSocket) {
                    String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                    Log.info("{} \"{}\": pull requested", WSFeedbackGroup.this.getClass().getSimpleName(),
                            context.getInstanceName());
                    WebSockets.sendText(modelReturn, webSocket, null);
                }

                private void handleRegister(WebSocketChannel webSocket, Message parsedMsg) {
                    RegisterMessage rm = (RegisterMessage) parsedMsg;
                    cache.put(rm.getNodeName(), webSocket);
                    rcache.put(webSocket, rm.getNodeName());
                    if (isMaster()) {
                        Log.info("New client registered \"{}\"", rm.getNodeName());
                        ContainerRoot modelToApply = cloner.clone(modelService.getCurrentModel().getModel());
                        if (rm.getModel() != null && !rm.getModel().equals("null")) {
                            // new registered model has a model to
                            // share: merging it locally
                            ContainerRoot recModel = (ContainerRoot) jsonModelLoader.loadModelFromString(rm.getModel())
                                    .get(0);
                            TraceSequence tseq = compare.merge(recModel, modelToApply);
                            Log.info("Merging his model with mine...", ((RegisterMessage) parsedMsg).getNodeName());
                            tseq.applyOn(recModel);
                            modelToApply = recModel;
                        }
                        if (checkFilter(((RegisterMessage) parsedMsg).getNodeName())) {
                            // add onConnect logic
                            try {
                                Log.debug("onConnect KevScript to process:");
                                final String onConnectKevs = tpl(onConnect, rm.getNodeName());
                                if (!onConnectKevs.trim().isEmpty()) {
                                    Log.debug("===== onConnect KevScript =====");
                                    Log.debug(onConnectKevs);
                                    Log.debug("===============================");
                                    kevsService.execute(onConnectKevs, modelToApply);
                                } else {
                                    Log.debug("onConnect KevScript empty");
                                }
                            } catch (Exception e) {
                                Log.error(
                                        "Unable to parse onConnect KevScript. Broadcasting model without onConnect process.",
                                        e);
                            } finally {
                                applyAndBroadcast(modelToApply);
                            }
                        } else {
                            applyAndBroadcast(modelToApply);
                        }
                    }
                }

                private void handlePush(String msg, Message parsedMsg) {
                    final PushMessage pm = (PushMessage) parsedMsg;

                    if (pm.getUid() == null) {
                        final String uid = new UID().toString();
                        Log.info("New push UID generated : " + uid);
                        pm.setUid(uid);
                    }
                    try {
                        Log.info("{} \"{}\": push received, applying locally...",
                                WSFeedbackGroup.this.getClass().getSimpleName(), context.getInstanceName());
                        ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                        if (hasMaster()) {
                            if (isMaster()) {
                                int count = broadcastModel(pm);

                                if (count > 0) {
                                    Log.info("Broadcast model over {} client{}", count, (count > 1) ? "s" : "");
                                    // TODO define timeout
                                }
                            }
                        } else {
                            Log.info("No master specified, model will NOT be send to all other nodes");
                        }

                        modelService.update(model, new UpdateCallback() {
                            @Override
                            public void run(Boolean applied) {
                                Log.info("{} \"{}\" update result: {}", WSFeedbackGroup.this.getClass().getSimpleName(),
                                        context.getInstanceName(), applied);
                                notifyDeploymentSuccess(context.getNodeName(), pm.getUid(), applied);
                            }

                        });
                    } catch (Exception e) {
                        Log.warn("{} \"{}\" received a malformed push message '{}'",
                                WSFeedbackGroup.this.getClass().getSimpleName(), context.getInstanceName(), msg);
                    }
                }

                private int broadcastModel(PushMessage pm) {
                    int count = 0;
                    for (WebSocketChannel ws : new HashSet<WebSocketChannel>(allConnectedClients)) {
                        count++;
                        if (ws.isOpen()) {
                            WebSockets.sendText(pm.toRaw(), ws, null);
                        }
                    }
                    return count;
                }

                private void applyAndBroadcast(ContainerRoot modelToApply) {
                    String recModelStr = serializer.serialize(modelToApply);
                    PushMessage pushMessage = new PushMessage(recModelStr, new UID().toString());

                    // update locally
                    modelService.update(modelToApply, applied -> Log.info("Merge model result: {}", applied));

                    // broadcast changes
                    Log.info("Broadcasting merged model to all connected clients");
                    for (WebSocketChannel client : new HashSet<WebSocketChannel>(allConnectedClients)) {
                        if (client.isOpen()) {
                            WebSockets.sendText(pushMessage.toRaw(), client, null);
                        }
                    }
                }

                @Override
                protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message)
                        throws IOException {
                    // Overriding onFullCloseMessage so that onFullTextMessage
                    // is called even though the data were sent by fragments
                }

                @Override
                protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
                        throws IOException {
                    String name = rcache.get(webSocketChannel);
                    if (name != null) {
                        cache.remove(name);
                    }
                    rcache.remove(webSocketChannel);
                    allConnectedClients.remove(webSocketChannel);
                }

                @Override
                protected void onError(WebSocketChannel webSocket, Throwable error) {
                    Log.error("{} \"{}\": {}", WSFeedbackGroup.this.getClass().getSimpleName(),
                            context.getInstanceName(), error.getMessage());
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
                            if (!onDisconnectKevs.trim().isEmpty()) {
                                Log.debug("===== onDisconnect KevScript =====");
                                Log.debug(onDisconnectKevs);
                                Log.debug("==================================");
                                modelService.submitScript(onDisconnectKevs,
                                        applied -> Log.info("{} \"{}\" onDisconnect result from {}: {}",
                                                WSFeedbackGroup.this.getClass().getSimpleName(),
                                                context.getInstanceName(), nodeName, applied));
                            } else {
                                Log.debug("onDisconnect KevScript empty");
                            }
                        } catch (Exception e) {
                            Log.error(
                                    "Unable to parse onDisconnect KevScript. No changes made after the disconnection of "
                                            + nodeName,
                                    e);
                        }
                    }
                    cache.remove(nodeName);
                }
                rcache.remove(ws);
                allConnectedClients.remove(ws);
            });
        }

        private String tpl(String tpl, String nodeName) {
            return tpl.replaceAll("\\{nodeName\\}", nodeName).replaceAll("\\{groupName\\}", context.getInstanceName());
        }
    }

    /**
     * Save the deployment status. Either by sending it to the master or by
     * saving it is the code is runned in the master node.
     * 
     * @param node
     *            the node namde
     * @param uid
     *            the uid of the deployed med
     * @param result
     *            the result of the deployment.
     */
    private void notifyDeploymentSuccess(String node, String uid, Boolean result) {
        if (this.isMaster()) {
            this.saveDeploymentResult(node, uid, result);
        } else {
            this.sendDeploymentResultToMaster(node, uid, result);
        }

    }

    private boolean sendDeploymentResultToMaster(String node, String uid, Boolean result) {
        if (masterClient != null && masterClient.isOpen()) {
            final ResultMessage pushMessage = new ResultMessage(node, uid, result);
            WebSockets.sendText(pushMessage.toRaw(), masterClient, null);
            return true;
        } else {
            diverge.set(true);
            Log.warn("Could not join master node : {}, diverge locally", master);
            return false;
        }
    }

    private void saveDeploymentResult(final String node, final String uid, final Boolean result) {
        int expectedNumberOfNodes = countExpectedNumberOfNode();
        resultTaskService.initUid(uid, expectedNumberOfNodes);
        // overriding is not controlled yet, a node can submit a result twice
        // for the same deployment UID.
        resultTaskService.startTimer(node, uid, result);
        resultTaskService.checkStatus();
    }

    private int countExpectedNumberOfNode() {
        int ret;
        if (strict) {
            final List<Group> lastModel = modelService.getCurrentModel().getModel().getGroups();

            Group current = null;
            for (Group g : lastModel) {
                if (g.getName().equals(context.getInstanceName())) {
                    current = g;
                    break;
                }
            }

            if (current != null && current.getSubNodes() != null) {
                ret = current.getSubNodes().size();
            } else {
                ret = 0;
            }
        } else {
            ret = cache.keySet().size();
        }

        return ret;
    }

    @Start
    public void startWSGroup() {
        modelService.registerModelListener(this);
        if (this.hasMaster()) {
            if (this.isMaster()) {
                this.resultTaskService = new ResultTaskService(this.delay);
                serverHandler = Undertow.builder().addHttpListener(port, "0.0.0.0")
                        .setHandler(websocket(new InternalWebSocketServer())).build();
                serverHandler.start();
                Log.info(WSFeedbackGroup.this.getClass().getSimpleName() + " \"{}\" listen on {}",
                        context.getInstanceName(), port);
            } else {
                scheduledThreadPool = Executors.newScheduledThreadPool(1);
                scheduledThreadPool.scheduleAtFixedRate(this, 0, 3000, TimeUnit.MILLISECONDS);
            }
        } else {
            this.resultTaskService = new ResultTaskService(this.delay);
            serverHandler = Undertow.builder().addHttpListener(port, "0.0.0.0")
                    .setHandler(websocket(new InternalWebSocketServer())).build();
            serverHandler.start();
            Log.info(WSFeedbackGroup.this.getClass().getSimpleName() + " \"{}\" listen on {}",
                    context.getInstanceName(), port);
        }
    }

    private Set<WebSocketChannel> allConnectedClients = Collections.synchronizedSet(new HashSet<>());
    private Map<String, WebSocketChannel> cache = new ConcurrentHashMap<String, WebSocketChannel>();
    private Map<WebSocketChannel, String> rcache = new ConcurrentHashMap<WebSocketChannel, String>();

    @Stop
    public void stopWSGroup() throws IOException, InterruptedException {

        if (this.resultTaskService != null) {
            resultTaskService.cleanup();
            this.resultTaskService = null;
        }

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

    @Override
    public boolean initUpdate(UpdateContext context) {
        return isMaster() || context.getCallerPath().equals(this.context.getPath())
                || !pushToMaster(context.getProposedModel());

    }

    public boolean pushToMaster(ContainerRoot model) {
        if (masterClient != null && masterClient.isOpen()) {
            final PushMessage pushMessage = new PushMessage(jsonModelSaver.serialize(model), new UID().toString());
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
                    PushMessage pushMessage = new PushMessage(currentModel, new UID().toString());
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
                    // localize master node
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
                                    if (net.getName().toLowerCase().contains("ip")
                                            || prop.getName().toLowerCase().contains("ip")) {
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
                            masterClient = createWSClient(defaultIP, port, context.getNodeName(), modelService,
                                    diverge);
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

    public WebSocketChannel createWSClient(final String ip, final String port, String currentNodeName,
            final ModelService modelService, final AtomicBoolean diverge) throws IOException {
        Xnio xnio = Xnio.getInstance(io.undertow.websockets.client.WebSocketClient.class.getClassLoader());
        XnioWorker worker = xnio.createWorker(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2)
                .set(Options.CONNECTION_HIGH_WATER, 1000000).set(Options.CONNECTION_LOW_WATER, 1000000)
                .set(Options.WORKER_TASK_CORE_THREADS, 30).set(Options.WORKER_TASK_MAX_THREADS, 30)
                .set(Options.TCP_NODELAY, true).set(Options.CORK, true).getMap());
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
                    Log.warn(WSFeedbackGroup.this.getClass().getSimpleName() + " \"{}\" unknown message '{}'",
                            context.getInstanceName(), msg);
                } else {
                    switch (parsedMsg.getType()) {
                    case PUSH_TYPE:
                        // push from master
                        diverge.set(false);
                        PushMessage pm = (PushMessage) parsedMsg;
                        ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                        modelService.update(model, new UpdateCallback() {
                            @Override
                            public void run(Boolean applied) {
                                Log.info(WSFeedbackGroup.this.getClass().getSimpleName() + " \"{}\" update result: {}",
                                        context.getInstanceName(), applied);
                                notifyDeploymentSuccess(context.getNodeName(), pm.getUid(), applied);
                            }
                        });
                        break;
                    default:
                        Log.warn(WSFeedbackGroup.this.getClass().getSimpleName() + " \"{}\" unhandled message '{}'",
                                context.getInstanceName(), msg);
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
    public void modelUpdated() {
    }

    @Override
    public void preRollback(UpdateContext context) {
    }

    @Override
    public void postRollback(UpdateContext context) {
    }
}
