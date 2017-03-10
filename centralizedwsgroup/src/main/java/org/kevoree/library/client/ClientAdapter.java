package org.kevoree.library.client;

import com.pusher.java_websocket.client.WebSocketClient;
import com.pusher.java_websocket.handshake.ServerHandshake;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.FragmentFacade;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.StringUtils;
import org.kevoree.log.Log;

import java.net.ConnectException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Creates a WebSocket client that tries to connect to the master server
 * Uses a ClientFragment instance in order to handle the appropriate actions
 *
 * Created by leiko on 1/10/17.
 */
public class ClientAdapter implements FragmentFacade {

    private long reconnectDelay = 3000;
    private WebSocketClient client;
    private ClientFragment fragment;
    private CentralizedWSGroup instance;
    private ScheduledExecutorService executorService;

    public ClientAdapter(CentralizedWSGroup instance) {
        this.instance = instance;
        this.fragment = new ClientFragment(instance);
    }

    @Override
    public void start() {
        this.client = new WebSocketClient(instance.getURI()) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.info("[{}][client] connected to {}", instance.getName(), uri);
                Protocol.RegisterMessage msg = fragment.register();
                client.send(msg.toRaw());
            }

            @Override
            public void onMessage(String message) {
                Protocol.Message pMsg = Protocol.parse(message);
                if (pMsg != null) {
                    if (fragment.isRegistered()) {
                        // registered client
                        switch (pMsg.getType()) {
                            case Protocol.PUSH_TYPE:
                                fragment.push((Protocol.PushMessage) pMsg);
                                break;

                            default:
                                Log.warn("[{}][client] ignoring message type \"{}\" send by master (state: registered)",
                                        instance.getName(), Protocol.getTypeName(pMsg.getType()));
                                break;
                        }
                    } else {
                        // unregistered client
                        switch (pMsg.getType()) {
                            case Protocol.REGISTERED_TYPE:
                                fragment.registered();
                                break;

                            default:
                                Log.warn("[{}][client] ignoring message type \"{}\" send by master (state: not yet registered)",
                                        instance.getName(), Protocol.getTypeName(pMsg.getType()));
                                break;
                        }
                    }
                } else {
                    Log.warn("[{}][client] unable to parse message: {}", instance.getName(),
                            StringUtils.shrink(message, 20));
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                fragment.unregister();
                if (code != 1000) {
                    reconnect();
                } else {
                    Log.info("[{}][client] connection closed with {} (code={},reason={})", instance.getName(), uri, code,
                            !reason.isEmpty() ? reason : "normal");
                }
            }

            @Override
            public void onError(Exception ex) {
                if (!(ex instanceof ConnectException)) {
                    ex.printStackTrace();
                } else {
                    Log.warn("[{}][client] unable to connect to {}", instance.getName(), uri);
                }
            }
        };

        Log.debug("[{}][client] trying to connect to {}", instance.getName(), instance.getURI());
        client.connect();
    }

    @Override
    public void close() {
        client.close();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        Log.debug("[{}][client] connection closed", instance.getName(), instance.getURI());
    }

    public void setReconnectDelay(long ms) {
        this.reconnectDelay = ms;
    }

    private void reconnect() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(this::start, reconnectDelay, TimeUnit.MILLISECONDS);
        Log.trace("[{}][client] will retry connection in {}ms", instance.getName(), reconnectDelay);
    }
}
