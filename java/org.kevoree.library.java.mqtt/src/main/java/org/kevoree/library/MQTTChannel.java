package org.kevoree.library;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;
import org.kevoree.annotation.*;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.net.URISyntaxException;

/**
 * Created by duke on 6/3/14.
 */
@ChannelType
public class MQTTChannel implements ChannelDispatch, Listener {

    @KevoreeInject
    Context context;

    @KevoreeInject
    ChannelContext channelContext;

    @Param(defaultValue = "tcp://mqtt.kevoree.org:81")
    String broker;

    @Update
    public void update() throws URISyntaxException {
        stop();
        start();
    }

    private MQTT mqtt;

    private CallbackConnection connection;

    private static final String KEVOREE_PREFIX = "kev/";

    private String topicName;

    public String getFQN() {
        return context.getInstanceName() + "@" + context.getNodeName();
    }

    @Start
    public void start() throws URISyntaxException {

        String clientID = KEVOREE_PREFIX + context.getInstanceName() + "_" + context.getNodeName();
        if (clientID.length() > 23) {
            clientID = clientID.substring(0, 23);
        }
        topicName = KEVOREE_PREFIX + context.getInstanceName();

        mqtt = new MQTT();
        mqtt.setClientId(clientID);
        mqtt.setCleanSession(true);
        mqtt.setHost(broker);

        connection = mqtt.callbackConnection();
        connection.listener(this);

        connection.connect(new org.fusesource.mqtt.client.Callback<Void>() {
            public void onFailure(Throwable value) {
                Log.error("MQTT connexion error ", value);
            }

            public void onSuccess(Void v) {
                Topic[] topics = {new Topic(topicName, QoS.AT_LEAST_ONCE)};
                connection.subscribe(topics, new org.fusesource.mqtt.client.Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                        Log.info("MQTT Channel " + getFQN() + " connected ");
                    }

                    public void onFailure(Throwable value) {
                        Log.error("MQTT subscription error ", value);
                    }
                });
            }
        });

    }

    @Stop
    public void stop() {
        connection.kill(new org.fusesource.mqtt.client.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                //TODO
            }

            @Override
            public void onFailure(Throwable value) {
                //TODO
            }
        });
        connection = null;
        mqtt = null;
    }


    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
        for (Port p : channelContext.getLocalPorts()) {
            p.call(body.utf8().toString(), null);//TODO callback retain strategy
        }
        ack.run();
    }

    @Override
    public void onFailure(Throwable value) {
        Log.error("MQTT error ", value);
    }

    @Override
    public void dispatch(Object payload, org.kevoree.api.Callback callback) {
        /*for (Port p : channelContext.getLocalPorts()) {
            p.call(payload, callback);
        }
        if (!channelContext.getRemotePortPaths().isEmpty()) {
        */
        connection.publish(topicName, payload.toString().getBytes(), QoS.AT_LEAST_ONCE, false, new org.fusesource.mqtt.client.Callback<Void>() {
            public void onSuccess(Void v) {
                Log.debug("message published on {}", topicName);
            }

            public void onFailure(Throwable value) {
                Log.error("Error while sending mqtt message", value);
            }
        });
        /* } */
    }
}
