package org.kevoree.library.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateContext;

/**
 * Created by duke on 6/3/14.
 */
public class MQTTGroup implements ModelListener, MqttCallback {

    @KevoreeInject
    Context context;

    @Param(defaultValue = "tcp://iot.eclipse.org:1883")
    String broker;

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

    @Update
    public void update() throws MqttException {
        stop();
        start();
    }

    @Override
    public boolean preUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean initUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public void modelUpdated() {

    }

    @Override
    public void preRollback(UpdateContext context) {

    }

    @Override
    public void postRollback(UpdateContext context) {

    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
