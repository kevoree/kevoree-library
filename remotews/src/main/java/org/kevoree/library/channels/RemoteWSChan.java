package org.kevoree.library.channels;

import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.XNIOException;
import fr.braindead.websocket.client.ReconnectWebSocketClient;
import fr.braindead.websocket.client.SimpleReconnectWebSocketClient;
import fr.braindead.websocket.client.WebSocketClient;
import org.kevoree.KevoreeParamException;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by leiko on 2/9/17.
 */
@ChannelType(version = 1, description = "A Kevoree chan that uses an external remote WebSocket broadcast server to share messages")
public class RemoteWSChan implements ChannelDispatch {

    @KevoreeInject
    private ChannelContext context;

    @Param(optional = false)
    private String host;

    @Param
    private int port = 80;

    @Param
    private String path = "/";

    @Param(optional = false)
    private String uuid;

    private String baseUri;
    private Map<String, WebSocket> receivers;
    private Map<String, WebSocket> dispatchers;

    public RemoteWSChan() {
        this.baseUri = null;
        this.receivers = new HashMap<>();
        this.dispatchers = new ConcurrentHashMap<>();
    }

    @Start
    public void start() throws KevoreeParamException, XNIOException, UnsupportedEncodingException {
        baseUri = this.getBaseURI();

        for (Port port : this.context.getLocalInputs()) {
            String path = port.getPath().substring(1);
            URI topic = URI.create(baseUri + URLEncoder.encode(path, "utf8"));
            WebSocketClient client = new ReconnectWebSocketClient(topic) {
                @Override
                public void onOpen() {
                    Log.debug("\"{}\" listening on topic \"{}\"", context.getInstanceName(), baseUri + path);
                }

                @Override
                public void onMessage(String message) {
                    context.getLocalInputs().forEach(port -> port.send(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code == 1000) {
                        Log.debug("\"{}\" closed connection with topic \"{}\"", context.getInstanceName(), baseUri + path);
                    } else {
                        Log.warn("\"{}\" lost connection with topic \"{}\"", context.getInstanceName(), baseUri + path);
                    }
                }

                @Override
                public void onError(Throwable ex) {
                    if (!(ex instanceof ConnectException)) {
                        Log.warn("\"{}\" ", ex, context.getInstanceName());
                    }
                }
            };

            this.receivers.put(port.getPath(), client);
            Log.debug("\"{}\" trying to connect to topic \"{}\"...", context.getInstanceName(), baseUri + path);
            // if connect() throws XNIO exception then it really not cool so..blow the component up
            client.connect();
        }

        for (Port port : this.context.getRemoteInputs()) {
            if (!this.dispatchers.containsKey(port.getPath())) {
                Log.debug("\"{}\" creates new dispatcher for topic \"{}\"", context.getInstanceName(), baseUri + port.getPath().substring(1));
                try {
                    URI uri = URI.create(baseUri + URLEncoder.encode(port.getPath().substring(1), "utf8"));
                    WebSocketClient dispatcher = new SimpleReconnectWebSocketClient(uri);
                    this.dispatchers.put(port.getPath(), dispatcher);
                    dispatcher.connect();
                } catch (XNIOException | UnsupportedEncodingException e) {
                    Log.error("\"{}\" unable to connect dispatcher to topic \"{}\"", e, context.getInstanceName(), baseUri + port.getPath().substring(1));
                }
            }
        }
    }

    @Stop
    public void stop() {
        // clean receivers
        this.receivers.values().forEach(client -> {
            try { client.close(); } catch (IOException ignore) {}
        });
        this.receivers.clear();

        // clean dispatchers
        this.dispatchers.values().forEach(client -> {
            try { client.close(); } catch (IOException ignore) {}
        });
        this.dispatchers.clear();
    }

    @Update
    public void update() throws KevoreeParamException, XNIOException, UnsupportedEncodingException {
        this.stop();
        this.start();
    }

    @Override
    public void dispatch(String message, Callback callback) {
        // local dispatch is straightforward
        context.getLocalInputs().forEach(localInput -> localInput.send(message));

        // remote dispatch implies sending message to remote WebSocket broadcaster
        // using baseUri + remote input path as endpoint
        context.getRemoteInputs().forEach(remoteInput -> {
            WebSocket client = this.dispatchers.get(remoteInput.getPath());
            if (client != null) {
                if (client.isOpen()) {
                    client.send(message);
                } else {
                    Log.warn("\"{}\" unable to dispatch \"{}\" to topic \"{}\" (client not opened)",
                            context.getInstanceName(), message, baseUri + remoteInput.getPath().substring(1));
                }
            } else {
                Log.warn("\"{}\" unable to dispatch \"{}\" to topic \"{}\" (client unavailable)",
                        context.getInstanceName(), message, baseUri + remoteInput.getPath().substring(1));
            }
        });
    }

    private String getBaseURI() throws KevoreeParamException {
        if (host == null || host.isEmpty()) {
            throw new KevoreeParamException("Parameter \"host\" must be set in \""+this.context.getInstanceName()+"\"");
        }

        if (uuid == null || uuid.isEmpty()) {
            throw new KevoreeParamException("Parameter \"uuid\" must be set in \""+this.context.getInstanceName()+"\"");
        }

        if (uuid.matches("/")) {
            throw new KevoreeParamException("Parameter \"uuid\" must not contain slashes in \""+this.context.getInstanceName()+"\"");
        }

        if (path.startsWith("/")) {
            // remove slash at the beginning
            path = path.substring(1);
        }

        if (!path.isEmpty() && path.endsWith("/")) {
            // remove slash at the end
            path = path.substring(0, path.length() - 2);
        }

        if (path.isEmpty()) {
            return "ws://" + host + ":" + port + "/" + uuid + "/";
        } else {
            return "ws://" + host + ":" + port + "/" + path + "/" + uuid + "/";
        }
    }
}
