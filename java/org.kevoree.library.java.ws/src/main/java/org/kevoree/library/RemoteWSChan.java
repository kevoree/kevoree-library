package org.kevoree.library;

import fr.braindead.websocket.client.WebSocketClient;
import org.kevoree.Channel;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@ChannelType
public class RemoteWSChan implements ChannelDispatch {

    private static final int LOOP_BREAK = 3000;
    private static final JSONModelLoader loader = new JSONModelLoader(new DefaultKevoreeFactory());
    private static final JSONModelSerializer serializer = new JSONModelSerializer();

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private ChannelContext channelContext;

    @Param(optional = false)
    private String host;

    @Param(defaultValue = "80")
    private int port = 80;

    @Param(defaultValue = "/")
    private String path = "/";

    @Param(optional = false)
    private String uuid;

    private ScheduledExecutorService scheduledThreadPool;
    private Map<String, WebSocketClient> clients;
    private String url;

    @Start
    public void start() throws Exception {
        clients = new HashMap<>();
        url = getURI();

        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }
        Channel thisChan = (Channel) model.findByPath(context.getPath());
        Set<String> inputPaths = Util.getProvidedPortsPath(thisChan, context.getNodeName());

        scheduledThreadPool = Executors.newScheduledThreadPool(inputPaths.size());
        inputPaths.forEach(p -> {
            try {
                WSConnectionCmd runnable = new WSConnectionCmd(url + URLEncoder.encode(p, "utf8"));
                scheduledThreadPool.scheduleAtFixedRate(runnable, 0, LOOP_BREAK, TimeUnit.MILLISECONDS);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }

    @Stop
    public void stop() throws IOException, InterruptedException {
        this.url = null;

        if (scheduledThreadPool != null) {
            scheduledThreadPool.shutdownNow();
        }
        if (this.clients != null) {
            clients.values()
                    .stream()
                    .filter(client -> client != null)
                    .forEach(client -> {
                        try {
                            client.close();
                        } catch (IOException e) { /* ignore */ }
                    });
            this.clients = null;
        }
    }

    @Update
    public void update() throws Exception {
        stop();
        start();
    }

    @Override
    public void dispatch(String s, Callback callback) {
        ContainerRoot model = modelService.getCurrentModel().getModel();
        if (model != null) {
            Set<String> destPaths = new HashSet<>();
            channelContext.getLocalPorts().forEach(p -> {
                org.kevoree.Port port = (org.kevoree.Port) model.findByPath(p.getPath());
                if (port != null && port.getRefInParent().equals("provided")) {
                    destPaths.add(port.path());
                }
            });
            channelContext.getRemotePortPaths().forEach(p -> {
                org.kevoree.Port port = (org.kevoree.Port) model.findByPath(p);
                if (port != null && port.getRefInParent().equals("provided")) {
                    destPaths.add(port.path());
                }
            });

            destPaths.forEach(path -> {
                WebSocketClient client = clients.get(path);
                if (client != null && client.isOpen()) {
                    client.send(s);
                } else {
                    try {
                        client = new WebSocketClient(URI.create(url + URLEncoder.encode(path, "utf8"))) {
                            @Override
                            public void onOpen() {
                                clients.put(path, this);
                                this.send(s);
                            }

                            @Override
                            public void onMessage(String msg) {
                            }

                            @Override
                            public void onClose(int code, String reason) {
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        };
                        clients.put(path, client);

                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    private String getURI() throws Exception {
        if (host == null || host.trim().length() == 0) {
            throw new Exception("'host' attribute is not specified");
        }

        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-2);
        }

        if (path.isEmpty()) {
            return "ws://"+host+":"+port+"/"+uuid;
        } else {
            return "ws://"+host+":"+port+"/"+path+"/"+uuid;
        }
    }

    private class WSConnectionCmd implements Runnable {

        private String uri;
        private WebSocketClient client;

        public WSConnectionCmd(String uri) {
            this.uri = uri;
        }

        public void run() {
            try {
                if (context != null) {
                    if (client == null || !client.isOpen()) {
                        client = new WebSocketClient(URI.create(uri)) {
                            @Override
                            public void onMessage(String msg) {
                                List<Port> ports = channelContext.getLocalPorts();
                                for (Port p : ports) {
                                    p.send(msg, null);
                                }
                            }

                            @Override
                            public void onOpen() {}
                            @Override
                            public void onClose(int code, String reason) {}
                            @Override
                            public void onError(Exception e) {}
                        };
                    }
                }
            } catch (Exception e) {
                System.err.println("Something went wrong while connecting to "+uri);
                e.printStackTrace();
            }
        }
    }
}
