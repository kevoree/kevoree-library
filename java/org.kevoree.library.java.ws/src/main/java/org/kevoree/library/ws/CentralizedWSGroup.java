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
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.library.ws.exception.MalformedKevoreeModelException;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelLoader;
import org.kevoree.modeling.api.ModelSerializer;
import org.kevoree.modeling.api.json.JSONModelSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leiko on 25/03/14.
 */
@GroupType
@Library(name = "Java :: Groups")
public class CentralizedWSGroup {

    private static final String REGISTER = "register";
    private static final String PUSH = "push";
    private static final String PULL = "pull";
    private static final String DIFF = "diff";

    @KevoreeInject
    public ModelService modelService;

    @KevoreeInject
    Context context;

    @Param(optional = true, fragmentDependent = true)
    Integer port;

    private WebSocketServer server;
    private WebSocketClient client;

    private Map<WebSocket, String> connectedClients;

    @Start
    public void start() throws MalformedKevoreeModelException, UnknownHostException {
        this.checkNoMultiplePortSet();

        if (this.port != null) {
            // we want this instance to be the centralized server
            this.startServer();
        } else {
            // we want this instance to just be a client
            this.startClient();
        }
    }

    private void startServer() throws UnknownHostException {
        this.connectedClients = new HashMap<WebSocket, String>();

        this.server = new WebSocketServer(new InetSocketAddress(this.port)) {
            @Override
            public void onOpen(WebSocket ws, ClientHandshake ch) {
                CentralizedWSGroup.this.connectedClients.put(ws, "just-connected");
            }

            @Override
            public void onClose(WebSocket ws, int i, String s, boolean b) {
                connectedClients.remove(ws);
            }

            @Override
            public void onMessage(WebSocket ws, String msg) {
                if (msg.startsWith(REGISTER)) {
                    // register new connected client
                    String nodeName = msg.substring(REGISTER.length() + 1);
                    Log.info("New registered client node '{}'", nodeName);
                    connectedClients.put(ws, nodeName);

                } else if (msg.startsWith(PUSH)) {
                    // push model to current node platform
                    final String strModel = msg.substring(PUSH.length() + 1);
                    deployModelLocally(strModel);

                } else if (msg.startsWith(PULL)) {
                    // send model to requesting client
                    ModelSerializer serializer = new JSONModelSerializer();
                    Log.info("{} asked for a PULL", ws.getRemoteSocketAddress().toString().substring(1));
                    ws.send(serializer.serialize(modelService.getCurrentModel().getModel()));

                } else {
                    Log.warn("Action " + msg.split("/")[0] + " unknown.");
                }
            }

            @Override
            public void onError(WebSocket ws, Exception e) {
                connectedClients.remove(ws);
            }
        };

        this.server.start();
        Log.info("CentralizedWSGroup listening on " + this.port);
    }

    private void startClient() {
        WSClientQueue queue = new WSClientQueue(this.getServerAddresses(), 5000, new WSClientHandler() {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                String register = new StringBuilder()
                        .append(REGISTER)
                        .append("/")
                        .append(modelService.getNodeName())
                        .toString();
                this.send(register);
            }

            @Override
            public void onMessage(String s) {
                deployModelLocally(s);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void deployModelLocally(final String strModel) {
        try {
            ModelLoader loader = new JSONModelLoader();
            ContainerRoot model = (ContainerRoot) loader.loadModelFromString(strModel).get(0);
            modelService.update(model, new UpdateCallback() {
                @Override
                public void run(Boolean applied) {
                    Log.info("CentralizedWSGroup update result : " + applied);

                    if (server != null) {
                        // broadcast model to all connected clients
                        for (WebSocket clientSocket : connectedClients.keySet()) {
                            if (clientSocket.isOpen()) {
                                clientSocket.send(strModel);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkNoMultiplePortSet() throws MalformedKevoreeModelException {
        Group grp = modelService.getPendingModel().findGroupsByID(context.getInstanceName());
        int portDefined = 0;
        for (FragmentDictionary fDic : grp.getFragmentDictionary()) {
            DictionaryValue val = fDic.findValuesByID("port");
            if (val != null && val.getValue() != null && val.getValue().length() > 0) {
                portDefined++;
            }
        }

        if (portDefined > 1) {
            throw new MalformedKevoreeModelException(portDefined + " 'port' attributes defined in model for '" + context.getInstanceName() + "' group instance (must be only one)");
        } else if (portDefined == 0) {
            throw new MalformedKevoreeModelException("No 'port' attribute defined in model for '" + context.getInstanceName() + "' group instance (must be only one)");
        }
    }

    private List<String> getServerAddresses() {
        List<String> addresses = new ArrayList<String>();
        Integer port = null;
        Group grp = modelService.getPendingModel().findGroupsByID(context.getInstanceName());
        for (FragmentDictionary fDic : grp.getFragmentDictionary()) {
            DictionaryValue val = fDic.findValuesByID("port");
            if (val != null && val.getValue() != null && val.getValue().length() > 0) {
                port = Integer.parseInt(val.getValue());
                break;
            }
        }
        if (port != null) {
            ContainerNode node = modelService.getPendingModel().findNodesByID(modelService.getNodeName());
            for (NetworkInfo net : node.getNetworkInformation()) {
                for (NetworkProperty prop : net.getValues()) {
                    if (net.getName().toLowerCase().contains("ip") || prop.getName().toLowerCase().contains("ip")) {
                        addresses.add(new StringBuilder().append(prop.getValue()).append(":").append(port).toString());
                    }
                }
            }
        }
        return addresses;
    }

    @Stop
    public void stop() {
        if (this.server != null) {
            try {
                this.server.stop();
            } catch (IOException e) {
                Log.error(e.getMessage());
            } catch (InterruptedException e) {
                Log.error(e.getMessage());
            }
        }

        if (this.client != null) {
            this.client.close();
        }
    }
}
