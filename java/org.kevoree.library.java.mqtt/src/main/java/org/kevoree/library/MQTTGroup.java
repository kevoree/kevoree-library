package org.kevoree.library;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by duke on 6/3/14.
 */
@GroupType
public class MQTTGroup implements ModelListener, Listener {

    @KevoreeInject
    Context localContext;

    @KevoreeInject
    ModelService modelService;

    @Param(defaultValue = "tcp://mqtt.kevoree.org:81")
    String broker;

    private static final String KEVOREE_PREFIX = "kev/";

    private String topicName;

    private MQTT mqtt;

    private CallbackConnection connection;

    @Start
    public void start() throws URISyntaxException {

        Log.info("Starting MqttGroup, connecting to adress:{}", broker);

        String clientID = KEVOREE_PREFIX + localContext.getInstanceName() + "_" + localContext.getNodeName();
        if (clientID.length() > 23) {
            clientID = clientID.substring(0, 23);
        }
        topicName = KEVOREE_PREFIX + localContext.getInstanceName();

        mqtt = new MQTT();
        mqtt.setClientId(clientID);
        mqtt.setCleanSession(true);
        mqtt.setHost(broker);
        mqtt.setReconnectDelayMax(3000);
        connection = mqtt.callbackConnection();
        connection.listener(this);

        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable value) {
                Log.error("MQTT Group connexion error ", value);
            }

            public void onSuccess(Void v) {
                Topic[] topics = {new Topic(topicName, QoS.AT_LEAST_ONCE)};
                connection.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                        Log.info("MQTT Group " + getFQN() + " connected and subscribed to " + topicName);
                    }

                    public void onFailure(Throwable value) {
                        Log.error("MQTT Group subscription error ", value);
                    }
                });
            }
        });
        modelService.registerModelListener(this);
    }

    @Stop
    public void stop() {
        Log.info("Stopping MqttGroup on node {}", localContext.getNodeName());
        if(modelService != null) {
            modelService.unregisterModelListener(this);
        }
        if(connection != null) {
            final Semaphore lock = new Semaphore(0);
            connection.kill(new Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                   lock.release();
                }

                @Override
                public void onFailure(Throwable value) {
                    lock.release();
                }
            });
            try {
                lock.tryAcquire(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.error("MqttGroup disconnection did not complete within 2s.", e);
            }

        }
    }

    @Update
    public void update() throws URISyntaxException {
        if(!broker.equals(mqtt.getHost().toString())) {
            Log.info("MqttGroup reboot. Switching from {} to {}", mqtt.getHost().toString(), broker);
            stop();
            start();
        }
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
        if (!context.getCallerPath().equals(localContext.getPath())) {
        Log.info("Model Update local, send to all ...");
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

    private JSONModelLoader loader = new JSONModelLoader(new DefaultKevoreeFactory());

    private JSONModelSerializer saver = new JSONModelSerializer();

    private ModelCompare compare = new ModelCompare(new DefaultKevoreeFactory());

    public String getFQN() {
        return localContext.getInstanceName() + "@" + localContext.getNodeName();
    }

    private static final String sep = "#";

    public void sendToServer(ContainerRoot model) {
        Log.info("Send Model to MQTT topic , origin:{}",model.getGenerated_KMF_ID());
        try {
            final StringBuilder builder = new StringBuilder();
            builder.append(getFQN());
            builder.append(sep);
            builder.append(saver.serialize(model));

            connection.getDispatchQueue().execute(new Runnable() {
                public void run() {
                    connection.publish(topicName, builder.toString().getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
                        public void onSuccess(Void v) {
                            Log.debug("message published on {}", topicName);
                        }

                        public void onFailure(Throwable value) {
                            Log.error("Error while sending mqtt message", value);
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
        try {
            Log.info("MqttGroup for node {} just received a model", localContext.getNodeName());
            String payload = new String(body.utf8().toString());
            if (payload.startsWith("pull")) {
                Log.info("Pull receive, send back model");
                sendToServer(modelService.getCurrentModel().getModel());
            } else {
                int indexSep = payload.indexOf(sep);
                String originName = null;
                if (indexSep != -1) {
                    originName = payload.substring(0, indexSep);
                }
                if (originName == null || !getFQN().equals(originName)) {

                    String modelPayload;
                    if (originName == null) {
                        modelPayload = payload;
                    } else {
                        modelPayload = payload.substring(indexSep + 1, payload.length());
                    }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        ack.run();
    }

    @Override
    public void onFailure(Throwable value) {
        Log.error("MQTT error ", value);
    }
}
