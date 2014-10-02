package org.kevoree.library;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.log.Log;

import java.net.URISyntaxException;

/**
 * Created by leiko on 6/3/14.
 */
@ChannelType
public class MQTTChannel implements ChannelDispatch, Listener {

    private static final String KEVOREE_PREFIX = "kev/";

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ChannelContext channelContext;

    @Param(defaultValue = "mqtt.kevoree.org")
    private String host;

    @Param(defaultValue = "81")
    private int port;

    private MQTT mqtt;
    private CallbackConnection connection;

    @Start
    public void start() throws URISyntaxException {
        mqtt = new MQTT();
        mqtt.setHost(host, port);

        connection = mqtt.callbackConnection();
        connection.listener(this);

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
        connection.kill(new org.fusesource.mqtt.client.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {}

            @Override
            public void onFailure(Throwable value) {}
        });
        connection = null;
        mqtt = null;
    }

    @Update
    public void update() throws URISyntaxException {
        stop();
        start();
    }

    @Override
    public void onConnected() {
        final String topicName = KEVOREE_PREFIX + context.getInstanceName() + "_" + context.getNodeName();
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
    public void onDisconnected() {}

    @Override
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
        for (Port p : channelContext.getLocalPorts()) {
            p.call(body.utf8().toString(), null);
        }
        ack.run();
    }

    @Override
    public void onFailure(Throwable value) {
        Log.error("{} error: {}", context.getInstanceName(), value.getMessage());
    }

    @Override
    public void dispatch(Object payload, org.kevoree.api.Callback callback) {
        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }

        if (connection != null) {
            // remote dispatch
            for (String portPath : channelContext.getRemotePortPaths()) {
                String targetNodeName = ((ContainerNode) model.findByPath(portPath).eContainer().eContainer()).getName();

                // publish message over the different topics
                final String topicName = KEVOREE_PREFIX + context.getInstanceName() + "_" + targetNodeName;
                try {
                    connection.publish(topicName, payload.toString().getBytes(), QoS.AT_LEAST_ONCE, false, null);
                } catch (Exception e) {
                    Log.error("Something went wrong while {} published a message. (reason: {})", context.getInstanceName(), e.getMessage());
                }
            }

            // local dispatch
            for (Port p : channelContext.getLocalPorts()) {
                p.call(payload, callback);
            }
        } else {
            Log.debug("Cannot dispatch message. Channel {} appears to be stopped.", context.getInstanceName());
        }
    }
}
