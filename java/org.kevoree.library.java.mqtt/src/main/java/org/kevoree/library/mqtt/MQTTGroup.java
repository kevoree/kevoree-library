package org.kevoree.library.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.serializer.JSONModelSerializer;

/**
 * Created by duke on 6/3/14.
 */
@GroupType
public class MQTTGroup implements ModelListener, MqttCallback {

    @KevoreeInject
    Context localContext;

    @KevoreeInject
    ModelService modelService;

    @Param(defaultValue = "tcp://iot.eclipse.org:1883")
    String broker;

    private MqttClient client;

    private static final String KEVOREE_PREFIX = "kevoree/";

    private String topicName;

    @Start
    public void start() throws MqttException {
        modelService.registerModelListener(this);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        client = new MqttClient(broker, KEVOREE_PREFIX + localContext.getNodeName() + "_" + localContext.getInstanceName());
        client.setCallback(this);
        topicName = KEVOREE_PREFIX + localContext.getInstanceName();
        client.connect(connOpts);
        client.subscribe(topicName);
    }

    @Stop
    public void stop() throws MqttException {
        modelService.unregisterModelListener(this);
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

        Log.info("Model Update local, send to all ...");

        if (!context.getCallerPath().equals(localContext.getPath())) {
            sendToServer(context.getProposedModel());
        }
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

    private JSONModelLoader loader = new JSONModelLoader();

    private JSONModelSerializer saver = new JSONModelSerializer();

    private ModelCompare compare = new DefaultModelCompare();

    public String getFQN() {
        return localContext.getInstanceName() + "@" + localContext.getNodeName();
    }

    private static final String sep = "#";

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        int indexSep = payload.indexOf(sep);
        String originName = payload.substring(0, indexSep);
        if (!getFQN().equals(originName)) {
            String modelPayload = payload.substring(indexSep + 1, payload.length());
            final ContainerRoot model = (ContainerRoot) loader.loadModelFromString(modelPayload).get(0);
            boolean broad = false;
            if (model.findNodesByID(localContext.getNodeName()) == null) {
                broad = true;
                //Merge and update locally
                compare.merge(model, modelService.getCurrentModel().getModel()).applyOn(model);
            }
            final boolean finalBroad = broad;
            modelService.update(model, new UpdateCallback() {
                @Override
                public void run(Boolean applied) {
                    if (finalBroad) {
                        sendToServer(model);
                    }
                }
            });
        }
    }

    public void sendToServer(ContainerRoot model) {
        Log.info("Send to MQTT server ");
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(getFQN());
            builder.append(sep);
            builder.append(saver.serialize(model));
            MqttMessage message = new MqttMessage(builder.toString().getBytes());
            message.setQos(1);
            client.publish(topicName, message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
