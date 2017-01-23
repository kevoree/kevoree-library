package org.kevoree.library.server;

import com.pusher.java_websocket.WebSocket;
import com.pusher.java_websocket.handshake.ClientHandshake;
import com.pusher.java_websocket.server.WebSocketServer;
import org.kevoree.ContainerRoot;
import org.kevoree.api.handler.ModelListenerAdapter;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.FragmentFacade;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.ModelReducer;
import org.kevoree.library.util.StringUtils;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates a WebSocket server and handles incoming connections and messages
 * using a ServerFragment
 * Created by leiko on 1/10/17.
 */
public class ServerAdapter extends WebSocketServer implements FragmentFacade {

    private final ServerFragment fragment;
    private final CentralizedWSGroup instance;
    private final ConcurrentHashMap<WebSocket, String> clients;
    private final ModelListenerAdapter modelListener;

    public ServerAdapter(CentralizedWSGroup instance) {
        super(new InetSocketAddress(instance.getPort()));

        this.instance = instance;
        this.fragment = new ServerFragment(instance);
        this.clients = new ConcurrentHashMap<>();

        this.modelListener = new ModelListenerAdapter() {
            @Override
            public void modelUpdated() {
                Log.debug("[{}][master] ======= Model Updated =======", instance.getName());
                broadcast(instance.getModelService().getCurrentModel().getModel());
            }
        };
    }

    @Override
    public void start() {
        Log.info("[{}][master] listening on 0.0.0.0:{}", instance.getName(), instance.getPort());

        instance.getModelService().registerModelListener(this.modelListener);
        super.start();
    }

    public void broadcast(final ContainerRoot model) {
        final Collection<WebSocket> conns = Collections.synchronizedCollection(this.connections());
        synchronized (conns) {
            if (!conns.isEmpty()) {
                KevoreeFactory factory = new DefaultKevoreeFactory();
                JSONModelSerializer serializer = factory.createJSONSerializer();
                Log.info("[{}][master] broadcasting model to every clients (size={})", instance.getName(), conns.size());
                final boolean reduceModel = instance.isReduceModel();
                final String masterNodeName = instance.getContext().getNodeName();
                conns.forEach(conn -> {
                    String nameToLog = "";
                    ContainerRoot processedModel = model;
                    String id = clients.get(conn);

                    if (id != null) {
                        // this "conn" is a registered node
                        if (reduceModel) {
                            String name = fragment.getName(id);
                            nameToLog = " to node \"" + name + "\"";
                            Log.info("[{}][master] reducing model for master \"{}\" and client \"{}\"",
                                    instance.getName(), masterNodeName, name);
                            processedModel = ModelReducer.reduce(model, masterNodeName, name);
                        }
                    }

                    String modelStr = serializer.serialize(processedModel);
                    String pushMsg = new Protocol.PushMessage(modelStr).toRaw();

                    if (conn.isOpen()) {
                        conn.send(pushMsg);
                        Log.debug("[{}][master] pushed new model{}", instance.getName(), nameToLog);
                    } else {
                        Log.debug("[{}][master] unable to push new model{} (connection is closed)", instance.getName(), nameToLog);
                    }
                });
            }
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.debug("[{}][master] new client connected", instance.getName());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String id = clients.remove(conn);
        if (id != null) {
            fragment.close(id, code != 1000 || remote);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String id = clients.get(conn);
        Protocol.Message pMsg = Protocol.parse(message);

        if (pMsg != null) {
            if (id != null) {
                // registered node
                Log.debug("[{}][master] received message from node \"{}\" (id={},msg={})", instance.getName(),
                        fragment.getName(id), id, StringUtils.shrink(message, 20));

                switch (pMsg.getType()) {
                    case Protocol.PULL_TYPE:
                        String modelStr = fragment.pull(id);
                        conn.send(modelStr);
                        break;

                    default:
                        Log.warn("[{}][master] protocol message type \"{}\" send by registered node (ignored)", instance.getName(),
                                Protocol.getTypeName(pMsg.getType()));
                        break;
                }

            } else {
                // unknown client
                Log.debug("[{}][master] received message from unknown client (msg={})", instance.getName(),
                        StringUtils.shrink(message, 20));
                switch (pMsg.getType()) {
                    case Protocol.REGISTER_TYPE:
                        id = fragment.register((Protocol.RegisterMessage) pMsg);
                        clients.put(conn, id);
                        conn.send(new Protocol.RegisteredMessage().toRaw());
                        break;

                    case Protocol.PULL_TYPE:
                        String modelStr = fragment.pull(conn.getRemoteSocketAddress().toString());
                        conn.send(modelStr);
                        break;

                    case Protocol.PUSH_TYPE:
                        fragment.push(conn.getRemoteSocketAddress().toString(), (Protocol.PushMessage) pMsg);
                        break;

                    default:
                        Log.warn("[{}][master] protocol message type \"{}\" sent by unregistered client (ignored)", instance.getName(),
                                Protocol.getTypeName(pMsg.getType()));
                        break;
                }
            }
        } else {
            Log.warn("[{}][master] unable to parse message: {}", instance.getName(),
                    StringUtils.shrink(message, 20));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String id = clients.get(conn);
        if (id == null) {
            Log.warn("[{}][master] error for unknown client", instance.getName());
        } else {
            Log.warn("[{}][master] error for node \"{}\" (id={})", instance.getName(), fragment.getName(id), id);
        }
    }

    @Override
    public void close() {
        instance.getModelService().unregisterModelListener(this.modelListener);

        // close all connections cleanly
        this.connections().forEach(WebSocket::close);

        try {
            super.stop(5000);
            Log.debug("[{}][master] server stopped", instance.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
