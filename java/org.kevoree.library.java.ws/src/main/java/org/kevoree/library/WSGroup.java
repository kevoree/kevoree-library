package org.kevoree.library;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.kevoree.*;
import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.kevoree.pmodeling.api.trace.TraceSequence;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.kevoree.library.Protocol.*;

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

    @Param(optional = true, fragmentDependent = true, defaultValue = "9000")
    Integer port;

    public void setMaster(String master) {
        this.master = master;
    }

    @Param(optional = true)
    String master;

    public void setPort(Integer port) throws IOException, InterruptedException {
        this.port = port;
        if (running) {
            serverHandler.stop();
            serverHandler = null;
            serverHandler = new InternalWebSocketServer(new InetSocketAddress(port));
            modelService.registerModelListener(this);
            serverHandler.start();
        }
    }

    private boolean running = false;
    private ScheduledExecutorService scheduledThreadPool;
    private WebSocketServer serverHandler;


    private class InternalWebSocketServer extends WebSocketServer {

        public InternalWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            String name = rcache.get(webSocket);
            if (name != null) {
                cache.remove(name);
            }
            rcache.remove(webSocket);
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            try {
                Message parsedMsg = Protocol.parse(s);
                if (parsedMsg == null) {
                    Log.warn(WSGroup.this.getClass().getSimpleName()+"  \"{}\" received an unknown message '{}'", context.getInstanceName(), s);
                } else {
                    switch (parsedMsg.getType()) {
                        case REGISTER_TYPE:
                            RegisterMessage rm = (RegisterMessage) parsedMsg;
                            cache.put(rm.getNodeName(), webSocket);
                            rcache.put(webSocket, rm.getNodeName());
                            if (isMaster()) {
                                if (rm.getModel() == null) {
                                    String currentModel = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                                    PushMessage pushMessage = new PushMessage(currentModel);
                                    Log.info("Sending my model to client \"{}\"", ((RegisterMessage) parsedMsg).getNodeName());
                                    webSocket.send(pushMessage.toRaw());
                                } else {
                                    //ok i've to merge locally
                                    ContainerRoot recModel = (ContainerRoot) jsonModelLoader.loadModelFromString(rm.getModel()).get(0);
                                    TraceSequence tseq = compare.merge(modelService.getCurrentModel().getModel(), recModel);
                                    Log.info("New client registered \"{}\". Merging his model with mine...", ((RegisterMessage) parsedMsg).getNodeName());
                                    modelService.submitSequence(tseq, new UpdateCallback() {
                                        @Override
                                        public void run(Boolean applied) {
                                            Log.info("Merge model result: {}", applied);
                                        }
                                    });
                                }
                            }
                            break;
                        case PULL_TYPE:
                            String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                            Log.info("{} \"{}\": pull requested", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                            webSocket.send(modelReturn);
                            break;
                        case PUSH_TYPE:
                            PushMessage pm = (PushMessage) parsedMsg;
                            try {
                                ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                                if (hasMaster()) {
                                    if (isMaster()) {
                                        int count = 0;
                                        for (WebSocket ws : cache.values()) {
                                            count++;
                                            if (ws.getReadyState() == WebSocket.READYSTATE.OPEN) {
                                                ws.send(((PushMessage) parsedMsg).getModel());
                                            }
                                        }

                                        if (count > 0) {
                                            Log.info("Broadcast model over {} client{}", count, (count > 1) ? "s" : "");
                                        }
                                    }
                                } else {
                                    Log.info("No master specified, model will NOT be send to all other nodes");
                                }

                                Log.info("{} \"{}\": push received, applying locally...", WSGroup.this.getClass().getSimpleName(), context.getInstanceName());
                                modelService.update(model, new UpdateCallback() {
                                    @Override
                                    public void run(Boolean applied) {
                                        Log.info("{} \"{}\" update result: {}", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), applied);
                                    }
                                });
                            } catch (Exception e) {
                                Log.warn("{} \"{}\" received a malformed push message '{}'", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), s);
                            }
                            break;
                        default:
                            Log.warn("{} \"{}\" unhandled message '{}'", WSGroup.this.getClass().getSimpleName(), context.getInstanceName(), s);
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            Log.error(WSGroup.this.getClass().getSimpleName()+" \"{}\": {}", context.getInstanceName(), e.getMessage());
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
            } catch (Exception ee) {
                e.printStackTrace();
            }
        }
    }

    @Start
    public void startWSGroup() {
        if (this.hasMaster()) {
            if (this.isMaster()) {
                serverHandler = new InternalWebSocketServer(new InetSocketAddress(port));
                serverHandler.start();
                Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" listen on {}", context.getInstanceName(), port);
            } else {
                modelService.registerModelListener(this);
                running = true;
                scheduledThreadPool = Executors.newScheduledThreadPool(1);
                scheduledThreadPool.scheduleAtFixedRate(this, 0, 3000, TimeUnit.MILLISECONDS);
            }
        } else {
            serverHandler = new InternalWebSocketServer(new InetSocketAddress(port));
            serverHandler.start();
            Log.info(WSGroup.this.getClass().getSimpleName()+" \"{}\" listen on {}", context.getInstanceName(), port);
        }
    }

    private Map<String, WebSocket> cache = new ConcurrentHashMap<String, WebSocket>();
    private Map<WebSocket, String> rcache = new ConcurrentHashMap<WebSocket, String>();

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
        if (masterClient != null && masterClient.getConnection().isOpen()) {
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
        if (masterClient != null && masterClient.getConnection().isOpen()) {
            PushMessage pushMessage = new PushMessage(jsonModelSaver.serialize(model));
            masterClient.send(pushMessage.toRaw());
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
            for (WebSocket clientSocket : cache.values()) {
                if (clientSocket.isOpen()) {
                    String currentModel = jsonModelSaver.serialize(context.getProposedModel());
                    PushMessage pushMessage = new PushMessage(currentModel);
                    clientSocket.send(pushMessage.toRaw());
                    Log.info("Forward to {}", rcache.get(clientSocket));
                } else {
                    Log.error("Disconnected Client " + rcache.get(clientSocket));
                }
            }
        }
        return true;
    }

    private WebSocketClient masterClient;

    public void run() {
        try {
            if (!isMaster() && context != null) {
                if (masterClient == null || !masterClient.getConnection().isOpen()) {
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
                                if (masterClient != null && masterClient.getConnection().isOpen()) {
                                    Log.info("Master connection opened on {}:{}", ip, port);
                                    return;
                                }
                            }
                            masterClient = createWSClient(defaultIP, port, context.getNodeName(), modelService, diverge);
                            if (masterClient != null && masterClient.getConnection().isOpen()) {
                                Log.info("Master connection opened on {}:{}", defaultIP, port);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error("error while connecting to master", e);
        }
    }

    public WebSocketClient createWSClient(String ip, String port, String currentNodeName, final ModelService modelService, final AtomicBoolean diverge) {
        final WebSocketClient[] client = new WebSocketClient[1];
        URI uri;
        if (ip.contains(":")) {
            uri = URI.create("ws://[" + ip + "]:" + port);
        } else {
            uri = URI.create("ws://" + ip + ":" + port);
        }
        client[0] = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
            }

            @Override
            public void onMessage(String message) {

                Protocol.Message parsedMsg = Protocol.parse(message);
                if (parsedMsg == null) {
                    Log.warn(WSGroup.this.getClass().getSimpleName()+" \"{}\" unknown message '{}'", context.getInstanceName(), message);
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
                            Log.warn(WSGroup.this.getClass().getSimpleName()+" \"{}\" unhandled message '{}'", context.getInstanceName(), message);
                            break;
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
        try {
            boolean result = client[0].connectBlocking();
            if (result) {
                String currentModel = null;
                if (diverge.get()) {
                    ContainerRoot model = modelService.getCurrentModel().getModel();
                    currentModel = jsonModelSaver.serialize(model);
                }
                client[0].send(new RegisterMessage(currentNodeName, currentModel).toRaw());
                return client[0];
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.trace("Error while creating WS client on {}", uri);
            return null;
        }
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
