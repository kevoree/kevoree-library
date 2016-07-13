package org.kevoree.library;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;
import org.kevoree.Channel;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.MBinding;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.log.Log;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by leiko on 6/3/14.
 */
@ChannelType(version = 1)
public class MQTTChannel implements ChannelDispatch, Listener {

    private static final String KEVOREE_PREFIX = "kev/";

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ChannelContext channelContext;

    @Param(defaultValue = "mqtt.kevoree.org", optional = false)
    private String host;

    @Param(defaultValue = "81", optional = false)
    private int port;

    @Param(optional = false)
    private String uuid;

    private MQTT mqtt;
    private CallbackConnection connection;

    @Start
    public void start() throws Exception {
        if (this.uuid == null || this.uuid.trim().isEmpty()) {
            throw new Exception("\"uuid\" attribute must be set");
        }
        this.uuid = uuid.replace("(/)*$", "");

        mqtt = new MQTT();
        mqtt.setHost(host, port);

        connection = mqtt.callbackConnection();
        connection.listener(this);

        Log.info("{} is trying to connect to {}:{} ...", context.getInstanceName(), host, port);
        connection.connect(new org.fusesource.mqtt.client.Callback<Void>() {
            public void onFailure(Throwable value) {
                Log.error("{} unable to connect to {}:{}", context.getInstanceName(), host, port);
            }

            public void onSuccess(Void v) {
                Log.info("{} connected to {}:{}", context.getInstanceName(), host, port);
            }
        });
    }

    @Stop
    public void stop() {
        if (connection != null) {
            connection.kill(new org.fusesource.mqtt.client.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    Log.info("{} closed connection with {}:{}", context.getInstanceName(), host, port);
                }

                @Override
                public void onFailure(Throwable value) {
                    Log.warn("{} failed to close connection with {}:{}", context.getInstanceName(), host, port);
                }
            });
        }
        connection = null;
        mqtt = null;
    }

    @Update
    public void update() throws Exception {
        stop();
        start();
    }

    @Override
    public void onConnected() {
        final String topicName = this.uuid + "/#";
        Topic[] topics = { new Topic(topicName, QoS.AT_LEAST_ONCE) };
        connection.subscribe(topics, new org.fusesource.mqtt.client.Callback<byte[]>() {
            public void onSuccess(byte[] qoses) {
                Log.info("{} subscribed to topic {}", context.getInstanceName(), topicName);
            }

            public void onFailure(Throwable value) {
                Log.error("{} unable to subscribe to topic {} (reason: {})", context.getInstanceName(), topicName, value.getMessage());
            }
        });
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onPublish(final UTF8Buffer topic, final Buffer body, final Runnable ack) {
        ContainerRoot model = modelService.getCurrentModel().getModel();
        if (model != null) {
            Channel chan = model.findHubsByID(context.getInstanceName());
            if (chan != null) {
                final Set<String> paths = new HashSet<String>();
                chan.getBindings().stream().filter(new Predicate<MBinding>() {
                    @Override
                    public boolean test(MBinding binding) {
                        return binding.getPort() != null
                                && binding.getPort().getRefInParent() != null
                                && binding.getPort().getRefInParent().equals("provided");
                    }
                }).forEach(new Consumer<MBinding>() {
                    @Override
                    public void accept(MBinding binding) {
                        ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                        if (node.getName().equals(context.getNodeName())) {
                            paths.add(binding.getPort().path());
                        }
                    }
                });
                paths.forEach(new Consumer<String>() {
                    @Override
                    public void accept(String path) {
                        String t = uuid+path;
                        if (t.equals(topic.toString())) {
                            for (Port p : channelContext.getLocalPorts()) {
                                p.send(body.utf8().toString(), null);
                                ack.run();
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onFailure(Throwable value) {
        Log.error("{} error: {}", context.getInstanceName(), value.getMessage());
    }

    @Override
    public void dispatch(final String payload, org.kevoree.api.Callback callback) {
        final ContainerRoot model = modelService.getCurrentModel().getModel();
        if (model != null && connection != null) {
            final Set<String> destPaths = new HashSet<String>();
            channelContext.getLocalPorts().forEach(new Consumer<Port>() {
                @Override
                public void accept(Port p) {
                    org.kevoree.Port port = (org.kevoree.Port) model.findByPath(p.getPath());
                    if (port != null && port.getRefInParent().equals("provided")) {
                        destPaths.add(port.path());
                    }
                }
            });
            channelContext.getRemotePortPaths().forEach(new Consumer<String>() {
                @Override
                public void accept(String p) {
                    org.kevoree.Port port = (org.kevoree.Port) model.findByPath(p);
                    if (port != null && port.getRefInParent().equals("provided")) {
                        destPaths.add(port.path());
                    }
                }
            });

            destPaths.forEach(new Consumer<String>() {
                @Override
                public void accept(String path) {
                    connection.publish(uuid+path, payload.getBytes(), QoS.AT_LEAST_ONCE, false, null);
                }
            });
        }

        // local dispatch
        for (Port p : channelContext.getLocalPorts()) {
            p.send(payload, callback);
        }

    }
}
