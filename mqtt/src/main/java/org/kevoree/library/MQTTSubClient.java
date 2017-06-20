package org.kevoree.library;

import java.net.URISyntaxException;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.annotation.Update;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.mqtt.message.Message;
import org.kevoree.log.Log;

import com.google.gson.Gson;

/**
 *
 * Created by leiko on 1/21/16.
 */
@ComponentType(version = 1, description = "Subscribes to the specified MQTT <strong>host</strong>:<strong>port</strong> and <strong>topic</strong> and broadcasts the published messages to its output ports")
public class MQTTSubClient implements Listener {

    @KevoreeInject
    private Context context;

    @Param(optional = false)
    private String host;

    @Param(optional = false)
    private Integer port;

    @Param
    private String topic = "/";

    @Output
    private Port onMsg;

    @Output
    private Port onTopicAndMsg;

    private MQTT client;
    private CallbackConnection connection;

    @Start
    public void start() throws URISyntaxException {
        if (host != null && !host.isEmpty() && port != null) {
            // create the MQTT client
            client = new MQTT();
            client.setHost(host, port);

            connection = client.callbackConnection();
            connection.listener(this);

            Log.info("{} is trying to connect to {}:{} ...", context.getInstanceName(), host, port);
            connection.connect(new org.fusesource.mqtt.client.Callback<Void>() {
                public void onFailure(Throwable value) {
                    Log.error("{} unable to connect to {}:{}", context.getInstanceName(), host, port);
                }

                public void onSuccess(Void v) {
                    Log.info("{} connected to {}:{}", context.getInstanceName(), host, port);
                    Topic[] topics = new Topic[] { new Topic(topic, QoS.EXACTLY_ONCE) };
                    connection.subscribe(topics, new Callback<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Log.info("{} subscribed to topic {}", context.getInstanceName(), topic);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.error(context.getInstanceName()+" failed to subscribe to topic "+topic, throwable);
                        }
                    });
                }
            });
        }
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
        client = null;
    }


    @Update
    public void update() throws URISyntaxException {
        stop();
        start();
    }

    @Override
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
        String message = body.utf8().toString();
        this.onMsg.send(message);
        ack.run();
        Message msg = new Message();
        msg.topic = topic.toString();
        msg.message = message;
        try {
            Gson gson = new Gson();
            String jsonMsg = gson.toJson(msg);
            this.onTopicAndMsg.send(jsonMsg);
        } catch (Exception e) {
            Log.error("{} unable to serialize topic and message to JSON, onTopicAndMsg port will not be used");
//            e.printStackTrace();
        }
    }

    @Override
    public void onConnected() {}

    @Override
    public void onDisconnected() {}

    @Override
    public void onFailure(Throwable throwable) {}
}
