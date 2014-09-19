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
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.json.JSONModelLoader;
import org.kevoree.modeling.api.json.JSONModelSerializer;
import org.kevoree.modeling.api.trace.TraceSequence;

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
    public Context localContext;

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
            Message parsedMsg = Protocol.parse(s);
            if (parsedMsg == null) {
                Log.error("Unknow Kevoree message {}", s);
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
                                webSocket.send(pushMessage.toRaw());
                            } else {
                                //ok i've to merge locally
                                ContainerRoot recModel = (ContainerRoot) jsonModelLoader.loadModelFromString(rm.getModel()).get(0);
                                TraceSequence tseq = compare.merge(modelService.getCurrentModel().getModel(), recModel);
                                modelService.submitSequence(tseq, new UpdateCallback() {
                                    @Override
                                    public void run(Boolean applied) {
                                        Log.info("Master merged deploy {}", applied);
                                    }
                                });
                            }
                        }
                        break;
                    case PULL_TYPE:
                        String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                        webSocket.send(modelReturn);
                        break;
                    case PUSH_TYPE:
                        PushMessage pm = (PushMessage) parsedMsg;
                        ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                        if (isMaster()) {
                            modelService.update(model, new UpdateCallback() {
                                @Override
                                public void run(Boolean applied) {
                                    Log.info("WSGroup update result : {}", applied);
                                }
                            });
                        } else {
                                /* Editor case injection, push to master */
                            Log.info("Update received from external , forward to master {}", master);
                            if (!pushToMaster(model)) {
                                Log.info("Master {}, not reachable, applied locally", master);
                                diverge.set(true);
                                modelService.update(model, new UpdateCallback() {
                                    @Override
                                    public void run(Boolean applied) {
                                        Log.info("WSGroup update result : {}", applied);
                                    }
                                });
                            }
                        }
                        break;
                    default:
                        Log.error("Unknow Kevoree message {}", s);
                        break;
                }
            }
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            Log.error("", e);
            try {
                webSocket.close();
            } catch (Exception ee) {
                e.printStackTrace();
            }
            if (rcache != null) {
                String name = rcache.get(webSocket);
                if (name != null) {
                    cache.remove(name);
                }
                rcache.remove(webSocket);
            }
        }
    }

    @Start
    public void startWSGroup() throws Exception {
        serverHandler = new InternalWebSocketServer(new InetSocketAddress(port));
        modelService.registerModelListener(this);
        serverHandler.start();
        Log.info("WSGroup listen on " + port);
        running = true;
        scheduledThreadPool = Executors.newScheduledThreadPool(1);
        scheduledThreadPool.scheduleAtFixedRate(this, 0, 3000, TimeUnit.MILLISECONDS);
    }

    private Map<String, WebSocket> cache = new ConcurrentHashMap<String, WebSocket>();
    private Map<WebSocket, String> rcache = new ConcurrentHashMap<WebSocket, String>();

    @Stop
    public void stopWSGroup() throws IOException, InterruptedException {
        scheduledThreadPool.shutdownNow();
        modelService.unregisterModelListener(this);
        if (serverHandler != null) {
            serverHandler.stop();
        }
        if (cache != null) {
            cache.clear();
        }
        if (rcache != null) {
            rcache.clear();
        }
        if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
            masterClients[0].closeBlocking();
            masterClients[0] = null;
        }
        running = false;

    }

    private static JSONModelLoader jsonModelLoader = new JSONModelLoader(new DefaultKevoreeFactory());
    private static JSONModelSerializer jsonModelSaver = new JSONModelSerializer();
    private static ModelCompare compare = new ModelCompare(new DefaultKevoreeFactory());

    private boolean isMaster() {
        if (master == null) {
            return false;
        } else {
            return localContext.getNodeName().equals(master);
        }
    }

    @Override
    public boolean preUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean initUpdate(UpdateContext context) {
        if (isMaster()) {
            return true;
        } else {
            if (!context.getCallerPath().equals(localContext.getPath())) {
                return !pushToMaster(context.getProposedModel());
            }
            return true;
        }

    }

    public boolean pushToMaster(ContainerRoot model) {
        if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
            PushMessage pushMessage = new PushMessage(jsonModelSaver.serialize(model));
            masterClients[0].send(pushMessage.toRaw());
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

    @Override
    public void modelUpdated() {

    }

    @Override
    public void preRollback(UpdateContext context) {

    }

    @Override
    public void postRollback(UpdateContext context) {

    }

    private final WebSocketClient[] masterClients = new WebSocketClient[1];

    public void run() {
        try {
            if (!isMaster() && localContext != null) {
                if (masterClients[0] == null || !masterClients[0].getConnection().isOpen()) {
                    ContainerRoot lastModel = modelService.getCurrentModel().getModel();
                    Group selfGroup = (Group) lastModel.findByPath(localContext.getPath());
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
                                masterClients[0] = createWSClient(ip, port, localContext.getNodeName(), modelService, diverge);
                                if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
                                    Log.info("Master connection opened on {}:{}", ip, port);
                                    return;
                                }
                            }
                            masterClients[0] = createWSClient(defaultIP, port, localContext.getNodeName(), modelService, diverge);
                            if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
                                Log.info("Master connection opened on {}:{}", defaultIP, port);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error("error while connecting to master", e);
        }
    }

    public static WebSocketClient createWSClient(String ip, String port, String currentNodeName, final ModelService modelService, final AtomicBoolean diverge) {
        final WebSocketClient[] client = new WebSocketClient[1];
        URI uri = null;
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
                    Log.error("Unknow Kevoree message {}", message);
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
                                    Log.info("WSGroup update result : {}", applied);
                                }
                            });
                            break;
                        default:
                            Log.error("Unknow Kevoree message {}", message);
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

}
