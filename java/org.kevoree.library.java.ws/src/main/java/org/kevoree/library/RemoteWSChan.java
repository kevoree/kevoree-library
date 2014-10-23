package org.kevoree.library;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.kevoree.Channel;
import org.kevoree.MBinding;
import org.kevoree.NamedElement;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.log.Log;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ChannelType
public class RemoteWSChan implements ChannelDispatch {

    @KevoreeInject Context context;
    @KevoreeInject ChannelContext channelContext;
    @KevoreeInject ModelService modelService;

    @Param private String path;
    @Param private int    port;
    @Param private String host;

    private WebSocketClient client;

    @Start
    public void start() {
        URI uri = constructURL(host, port, path);
        createClient(uri);
    }

    @Stop
    public void stop() {
        if (this.client != null) {
            try {
                this.client.closeBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Update
    public void update() {
        stop();
        start();
    }

    @Override
    public void dispatch(final Object payload, final Callback callback) {
        if (this.client != null && this.client.getReadyState() == WebSocket.READYSTATE.OPEN) {
            JsonObject msg = new JsonObject();
            msg.addProperty("action", "send");
            msg.addProperty("message", new String(payload.toString().getBytes()));
            JsonArray destPaths = new JsonArray();
            List<String> remotePortPaths = channelContext.getRemotePortPaths();
            for (String path : remotePortPaths) {
                destPaths.add(new JsonPrimitive(path));
            }
            msg.add("dest", destPaths);

            this.client.send(new Gson().toJson(msg));
        }
    }

    private URI constructURL(String host, int port, String path) {
        StringBuilder builder = new StringBuilder();

        if (host.contains(":")) {
            // IPv6
            builder.append("ws://[").append(host).append("]:");
        } else {
            // IPv4
            builder.append("ws://").append(host).append(":");
        }
        builder.append(port);
        if (path != null) {
            if (path.length() > 0 && !path.substring(0, 1).equals("/")) {
                builder.append("/");
            }
            builder.append(path);
        }

        return URI.create(builder.toString());
    }

    private void createClient(URI uri) {
        this.client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.info("'{}' connected to remote WebSocket server {}", context.getInstanceName(), uri);

                Channel chan = (Channel) modelService.getCurrentModel().getModel().findByPath(context.getPath());
                for (MBinding binding : chan.getBindings()) {
                    if (binding.getPort() != null && binding.getPort().getRefInParent().equals("provided")) {
                        if (((NamedElement) binding.getPort().eContainer().eContainer()).getName().equals(context.getNodeName())) {
                            JsonObject msg = new JsonObject();
                            msg.addProperty("action", "register");
                            msg.addProperty("id", binding.getPort().path());
                            this.send(new Gson().toJson(msg));
                        }
                    }
                }
            }

            @Override
            public void onMessage(String s) {
                List<Port> ports = channelContext.getLocalPorts();
                for (Port p : ports) {
                    p.call(s, null);
                }
            }

            @Override
            public void onError(Exception e) {}

            @Override
            public void onClose(int i, String reason, boolean b) {
                Log.info("'{}' connection lost with {} (reason: {})", context.getInstanceName(), uri, reason);
                try {
                    Thread.sleep(5000);
                    createClient(uri);
                } catch (InterruptedException e) {
                    /* ignore interrupt exception it is probably Kevoree Core
                     * shutting myself because I had to stop */
                }
            }
        };

        client.connect();
    }

    public static void main(String[] args) {
        // UGLY TEST
        RemoteWSChan chan = new RemoteWSChan();
        chan.context = new Context() {
            public String getPath() {
                return "hubs[chan]";
            }

            public String getNodeName() {
                return "node0";
            }

            public String getInstanceName() {
                return "chan";
            }
        };
        chan.channelContext = new ChannelContext() {
            @Override
            public List<Port> getLocalPorts() {
                return new ArrayList<Port>();
            }

            @Override
            public List<String> getRemotePortPaths() {
                List<String> list = new ArrayList<String>();
                list.add("nodes[node0]/components[printer]/provided[input]");
                return list;
            }
        };
        chan.host = "127.0.0.1";
        chan.port = 9001;
        chan.path = "";

        chan.start();
        chan.dispatch("test", null);
    }
}