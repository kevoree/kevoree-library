package org.kevoree.library.ws;

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
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.serializer.JSONModelSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.kevoree.library.ws.Protocol.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@GroupType
@Library(name = "Java :: Groups")
public class WSGroup implements ModelListener, Runnable {

    @KevoreeInject
    public Context localContext;

    @KevoreeInject
    public ModelService modelService;

    @Param(optional = true, fragmentDependent = true, defaultValue = "9000")
    Integer port;

    @Param(optional = true)
    String master;

    public void setPort(Integer port) throws IOException, InterruptedException {
        this.port = port;
        if (running) {
            serverHandler.stop();
            serverHandler.start();
        }
    }

    private boolean running = false;
    private ScheduledExecutorService scheduledThreadPool;
    private WebSocketServer serverHandler;

    @Start
    public void startWSGroup() throws UnknownHostException {
        serverHandler = new WebSocketServer(new InetSocketAddress(port)) {
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
                Protocol.Message parsedMsg = Protocol.parse(s);
                if (parsedMsg == null) {
                    Log.error("Unknow Kevoree message {}", s);
                } else {
                    switch (parsedMsg.getType()) {
                        case REGISTER_TYPE:
                            RegisterMessage rm = (RegisterMessage) parsedMsg;
                            cache.put(rm.getNodeName(), webSocket);
                            rcache.put(webSocket, rm.getNodeName());
                            if (isMaster()) {
                                String currentModel = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                                PushMessage pushMessage = new PushMessage(currentModel);
                                webSocket.send(pushMessage.toRaw());
                            }
                            break;
                        case PULL_TYPE:
                            String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
                            webSocket.send(modelReturn);
                            break;
                        case PUSH_TYPE:
                            PushMessage pm = (PushMessage) parsedMsg;
                            ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(pm.getModel()).get(0);
                            model.setGenerated_KMF_ID("master");
                            modelService.update(model, new UpdateCallback() {
                                @Override
                                public void run(Boolean applied) {
                                    Log.info("WSGroup update result : {}", applied);
                                }
                            });
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
                }

                rcache.remove(webSocket);
            }
        };
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

    private JSONModelLoader jsonModelLoader = new JSONModelLoader();
    private JSONModelSerializer jsonModelSaver = new JSONModelSerializer();

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

            if(context.getCallerPath().equals(localContext.getPath()))

            if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
                PushMessage pushMessage = new PushMessage(jsonModelSaver.serialize(context.getProposedModel()));
                masterClients[0].send(pushMessage.toRaw());
                return false;
            }
            Log.error("Could not join master node : {}, diverge locally", master);
            return true;
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
                    Log.error("Sync Client " + rcache.get(clientSocket));
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
                    if (selfGroup != null) {
                        FragmentDictionary masterDico = selfGroup.findFragmentDictionaryByID(master);
                        String defaultIP = "127.0.0.1";
                        String port = "9000";
                        if (masterDico != null) {
                            DictionaryValue val = masterDico.findValuesByID("port");
                            port = val.getValue();
                        }
                        List<String> addresses = new ArrayList<String>();
                        ContainerNode node = lastModel.findNodesByID(master);
                        if (node != null) {
                            for (NetworkInfo net : node.getNetworkInformation()) {
                                for (NetworkProperty prop : net.getValues()) {
                                    if (net.getName().toLowerCase().contains("ip") || prop.getName().toLowerCase().contains("ip")) {
                                        addresses.add(prop.getValue());
                                    }
                                }
                            }
                            for (String ip : addresses) {
                                masterClients[0] = createWSClient(ip, port, serverHandler, localContext.getNodeName());
                                if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
                                    Log.info("Master connection opened on {}:{}", ip, port);
                                    return;
                                }
                            }
                            masterClients[0] = createWSClient(defaultIP, port, serverHandler, localContext.getNodeName());
                            if (masterClients[0] != null && masterClients[0].getConnection().isOpen()) {
                                Log.info("Master connection opened on {}:{}", defaultIP, port);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error("", e);
        }
    }

    public static WebSocketClient createWSClient(String ip, String port, final WebSocketServer parent, String currentNodeName) {
        final WebSocketClient[] client = new WebSocketClient[1];
        URI uri = URI.create("ws://" + ip + ":" + port);
        client[0] = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
            }

            @Override
            public void onMessage(String message) {
                parent.onMessage(null, message);
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
                client[0].send(new RegisterMessage(currentNodeName).toRaw());
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
