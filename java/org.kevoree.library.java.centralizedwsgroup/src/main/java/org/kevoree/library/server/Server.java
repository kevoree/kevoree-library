package org.kevoree.library.server;

import com.pusher.java_websocket.WebSocket;
import com.pusher.java_websocket.handshake.ClientHandshake;
import com.pusher.java_websocket.server.WebSocketServer;
import org.kevoree.api.handler.ModelListenerAdapter;
import org.kevoree.library.FragmentFacade;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.ConnIdentity;
import org.kevoree.library.util.ShortId;
import org.kevoree.library.util.StringUtils;
import org.kevoree.log.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 *
 * Created by leiko on 1/10/17.
 */
public class Server implements FragmentFacade {

    private CentralizedWSGroup instance;
    private WebSocketServer wsServer;
    private HashMap<WebSocket, ConnIdentity> clients;

    public Server(CentralizedWSGroup instance) {
        this.instance = instance;
        this.clients = new HashMap<>();
    }

    @Override
    public void create() {
        this.wsServer = new WebSocketServer(new InetSocketAddress(instance.getPort())) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                ConnIdentity identity = new ConnIdentity();
                identity.id = ShortId.gen();
                Log.debug("[{}][master] (onOpen) new client {}", instance.getName(), identity.id);
                clients.put(conn, identity);
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                ConnIdentity identity = clients.remove(conn);
                Log.debug("[{}][master] (onClose) removing client {} (id={})", instance.getName(), identity.name,
                        identity.id);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                ConnIdentity identity = clients.get(conn);
                Log.debug("[{}][master] (onMessage) from {} (id={}) is: {}", instance.getName(), identity.name,
                        identity.id, StringUtils.shrink(message, 20));
                Protocol.Message pMsg = Protocol.parse(message);
                if (pMsg != null) {
                    switch (pMsg.getType()) {
                        case Protocol.PULL_TYPE:
                            PullHandler.process(conn, clients, instance);
                            break;

                        case Protocol.REGISTER_TYPE:
                            RegisterHandler.process(conn, clients, (Protocol.RegisterMessage) pMsg, instance);
                            break;

                        case Protocol.PUSH_TYPE:
                            PushHandler.process(conn, clients, (Protocol.PushMessage) pMsg, instance);
                            break;

                        default:
                            Log.warn("[{}][master] protocol message type \"{}\" not handled yet.", instance.getName(),
                                    Protocol.getTypeName(pMsg.getType()));
                            break;
                    }
                } else {
                    Log.warn("[{}][master] unable to parse message: {}", instance.getName(),
                            StringUtils.shrink(message, 20));
                }
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                ConnIdentity identity = clients.get(conn);
                Log.warn("[{}][master] (onError) from {} (id={})", instance.getName(), identity.name, identity.id);
                ex.printStackTrace();
            }
        };

        this.wsServer.start();
        Log.info("[{}][master] listening on 0.0.0.0:{}", instance.getName(), instance.getPort());

        instance.getModelService().registerModelListener(new ModelListenerAdapter() {
            @Override
            public void modelUpdated() {
                Log.debug("======= Model Updated =======");
            }
        });
//        instance.getKevoreeCore().on('deployed', function () {
//            var connectedNodes = [];
//            var factory = new kevoree.factory.DefaultKevoreeFactory();
//            var serializer = factory.createJSONSerializer();
//            var model = instance.getKevoreeCore().getCurrentModel();
//
//            if (model.generated_KMF_ID === server.modelId) {
//                server.modelId = null;
//                server.clients.forEach(function (client) {
//                    var clientNodeName = client2name[client.id];
//                    if (clientNodeName) {
//                        if (client.readyState === WebSocket.OPEN) {
//                            var group = instance.getModelEntity();
//                            var masterName = findMasterNode(group).name;
//                            var reducedModel = reducer(model, masterName, clientNodeName);
//                            var reducedModelStr = serializer.serialize(reducedModel);
//                            client.send(new PushMessage(reducedModelStr).toRaw());
//                            connectedNodes.push(clientNodeName);
//                        } else {
//                            // connection is not opened
//                            logger.warn('connection with "'+clientNodeName+'" is closed');
//                        }
//                    }
//                });
//                if (connectedNodes.length > 0) {
//                    logger.info('model sent to: ' + connectedNodes.join(', '));
//                }
//            } else {
//                // send serialized model to every connected clients that are not registered nodes (because they should be editors)
//                var modelStr = serializer.serialize(model);
//                server.clients.forEach(function (client) {
//                    if (!client2name[client.id]) {
//                        if (client.readyState === WebSocket.OPEN) {
//                            client.send(modelStr);
//                        }
//                    }
//                });
//            }
//        });
    }

    @Override
    public void close() {
        // TODO
        if (this.wsServer != null) {
            try {
                this.wsServer.stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
