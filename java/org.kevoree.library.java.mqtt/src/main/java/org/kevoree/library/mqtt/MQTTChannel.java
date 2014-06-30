package org.kevoree.library.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.log.Log;

/**
 * Created by duke on 6/3/14.
 */
@ChannelType
@Library(name = "Java")
public class MQTTChannel implements ChannelDispatch, MqttCallback {

    @KevoreeInject
    Context context;

    @KevoreeInject
    ChannelContext channelContext;

    @Param(defaultValue = "tcp://mqtt.kevoree.org:81")
    String broker;

    @Update
    public void update() throws MqttException {
        stop();
        start();
    }

    private MqttAsyncClient client;

    private static final String KEVOREE_PREFIX = "kev/";

    private String topicName;

    @Start
    public void start() throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        String clientID = KEVOREE_PREFIX + context.getNodeName() + "_" + context.getInstanceName();
        if (clientID.length() > 20) {
            clientID = clientID.substring(0, 20);
        }

        client = new MqttAsyncClient(broker, clientID, new MemoryPersistence());
        client.setCallback(this);
        topicName = KEVOREE_PREFIX + context.getInstanceName();
        client.connect(connOpts, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                try {
                    Log.info("MQTT Channel connected");
                    client.subscribe(topicName, 0);
                } catch (MqttException e) {
                    Log.error("mqtt error", e);
                }
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

                Log.error("mqtt error", throwable);
            }
        }).waitForCompletion();
    }

    @Stop
    public void stop() throws MqttException {
        client.setCallback(null);
        client.disconnect();
        client = null;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        try {
            client.connect();
        } catch (MqttException e) {
            Log.error("", e);
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        for (Port p : channelContext.getLocalPorts()) {
            p.call(new String(mqttMessage.getPayload()), null);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public void dispatch(Object payload, Callback callback) {
        /*for (Port p : channelContext.getLocalPorts()) {
            p.call(payload, callback);
        }
        if (!channelContext.getRemotePortPaths().isEmpty()) {
        */

        try {
            //MqttMessage message = new MqttMessage(payload.toString().getBytes());
            //message.setQos(1);
            //message.setRetained(false);
            try {
                if (!client.isConnected()) {
                    client.connect().waitForCompletion();
                }
                client.publish(topicName, payload.toString().getBytes(),1,false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // }
    }
}
