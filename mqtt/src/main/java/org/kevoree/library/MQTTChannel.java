package org.kevoree.library;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.service.ModelService;
import org.kevoree.log.Log;

/**
 * Created by leiko on 6/3/14.
 */
@ChannelType(version = 1, description = "A Kevoree channel that uses MQTT to broadcast messages")
public class MQTTChannel implements ChannelDispatch, Listener {

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private ChannelContext context;

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
                Log.error("{} unable to subscribe to topic {} (reason: {})", context.getInstanceName(), topicName,
                        value.getMessage());
            }
        });
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onPublish(final UTF8Buffer topic, final Buffer body, final Runnable ack) {
        context.getLocalInputs().forEach(localInput -> {
            if (topic.toString().equals(uuid + localInput.getPath())) {
                localInput.send(body.utf8().toString());
                ack.run();
            }
        });
    }

    @Override
    public void onFailure(Throwable value) {
        Log.error("{} error: {}", context.getInstanceName(), value.getMessage());
    }

    @Override
    public void dispatch(final String payload, Callback callback) {
        // local dispatch
        context.getLocalInputs().forEach(localInput -> localInput.send(payload));

        // remote dispatch
        if (connection != null) {
            context.getRemoteInputs().forEach(remoteInput ->
                    connection.publish(uuid + remoteInput.getPath(), payload.getBytes(), QoS.AT_LEAST_ONCE, false, null));
        }
    }
}
