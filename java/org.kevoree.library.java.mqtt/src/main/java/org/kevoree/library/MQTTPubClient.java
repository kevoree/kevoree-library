package org.kevoree.library;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.mqtt.message.Message;
import org.kevoree.log.Log;

import java.net.URISyntaxException;

/**
 *
 * Created by leiko on 1/21/16.
 */
@ComponentType(version = 1, description = "Publishes the messages it receives to its input ports to the specified MQTT <strong>host</strong>:<strong>port</strong> and <strong>topic</strong>")
public class MQTTPubClient {

    @KevoreeInject
    private Context context;

    @Param(optional = false)
    private String host;

    @Param(optional = false)
    private Integer port;

    @Param(defaultValue = "/")
    private String topic = "/";

    private MQTT client;
    private CallbackConnection connection;

    @Start
    public void start() throws URISyntaxException {
        if (host != null && !host.isEmpty() && port != null) {
            // create the MQTT client
            client = new MQTT();
            client.setHost(host, port);
            connection = client.callbackConnection();

            Log.info("{} is trying to connect to {}:{} ...", context.getInstanceName(), host, port);
            connection.connect(new org.fusesource.mqtt.client.Callback<Void>() {
                public void onFailure(Throwable throwable) {
                    Log.error(context.getInstanceName()+" failed to connect to "+host+":"+port, throwable);
                }

                public void onSuccess(Void v) {
                    Log.info("{} connected to {}:{}", context.getInstanceName(), host, port);
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
                public void onFailure(Throwable throwable) {
                    Log.error(context.getInstanceName()+" failed to close connection with "+host+":"+port, throwable);
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

    @Input
    public void pub(String msg) {
        if (this.connection != null) {
            this.connection.publish(topic, msg.getBytes(), QoS.EXACTLY_ONCE, false, null);
            Log.debug("{} publishing \"{}\" to \"{}\"", context.getInstanceName(), msg, topic);
        }
    }

    @Input
    public void jsonPub(String jsonMsg) {
        if (this.connection != null) {
            Gson gson = new Gson();
            try {
                Message msg = gson.fromJson(jsonMsg, Message.class);
                this.connection.publish(msg.topic, msg.message.getBytes(), QoS.EXACTLY_ONCE, false, null);
                Log.debug("{} publishing \"{}\" to \"{}\"", context.getInstanceName(), msg.message, msg.topic);
            } catch (Exception e) {
                Log.error("{} unable to parse incoming JSON message, message will not be published", context.getInstanceName());
            }
        }
    }
}
