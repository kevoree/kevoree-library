package org.kevoree.library.mqtt;

import org.eclipse.paho.client.mqttv3.*;
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

    @Param(defaultValue = "tcp://iot.eclipse.org:1883")
    String broker;

    @Update
    public void update() throws MqttException {
        stop();
        start();
    }

    private MqttClient client;

    private static final String KEVOREE_PREFIX = "kevoree/";

    private String topicName;

    @Start
    public void start() throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        client = new MqttClient(broker, KEVOREE_PREFIX + context.getNodeName() + "_" + context.getInstanceName());
        client.setCallback(this);
        topicName = KEVOREE_PREFIX + context.getInstanceName();
        client.connect(connOpts);
        client.subscribe(topicName);
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
            MqttMessage message = new MqttMessage(payload.toString().getBytes());
            message.setQos(1);
            try {
                client.publish(topicName, message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
       // }
    }
}
