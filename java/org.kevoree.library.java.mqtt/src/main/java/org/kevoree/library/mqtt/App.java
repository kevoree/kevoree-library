package org.kevoree.library.mqtt;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.net.URISyntaxException;

/**
 * Created by duke on 6/30/14.
 */
public class App {

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String clientID = "myID";
        MQTT mqtt = new MQTT();
        mqtt.setClientId(clientID);
        mqtt.setCleanSession(true);
        mqtt.setHost("tcp://localhost:1883");
        final CallbackConnection connection = mqtt.callbackConnection();
        connection.listener(new Listener() {
            public void onDisconnected() {
                connection.failure();
            }

            @Override
            public void onPublish(UTF8Buffer utf8Buffer, Buffer buffer, Runnable runnable) {
                System.out.println(buffer.utf8().toString());
                runnable.run();
            }


            public void onConnected() {

            }

            public void onFailure(Throwable value) {
                connection.kill(null); // a connection failure occured.
            }
        });


        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable value) {
                value.printStackTrace();
                //result.failure(value); // If we could not connect to the server.
            }

            // Once we connect..
            public void onSuccess(Void v) {

                // Subscribe to a topic
                Topic[] topics = {new Topic("foo", QoS.AT_LEAST_ONCE)};
                connection.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {

                        System.out.println("Message arrived :-) " + qoses.length);

                        System.out.println(">" + new String(qoses) + "|");

                        // The result of the subcribe request.
                    }

                    public void onFailure(Throwable value) {
                        value.printStackTrace();
                        // connection.kill(null); // subscribe failed.
                    }
                });


                // To disconnect..
                /*
                connection.disconnect(new Callback<Void>() {
                    public void onSuccess(Void v) {
                        // called once the connection is disconnected.
                    }

                    public void onFailure(Throwable value) {
                        // Disconnects never fail.
                    }
                });
                */
            }
        });

        Thread.sleep(2000);


        // Send a message to a topic
        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
            public void onSuccess(Void v) {
                System.out.println("Message published");
                // the pubish operation completed successfully.
            }

            public void onFailure(Throwable value) {
                value.printStackTrace();
                //connection.close(null); // publish failed.
            }
        });

        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
            public void onSuccess(Void v) {
                System.out.println("Message published");
                // the pubish operation completed successfully.
            }

            public void onFailure(Throwable value) {
                value.printStackTrace();
                //connection.close(null); // publish failed.
            }
        });
        connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
            public void onSuccess(Void v) {
                System.out.println("Message published");
                // the pubish operation completed successfully.
            }

            public void onFailure(Throwable value) {
                value.printStackTrace();
                //connection.close(null); // publish failed.
            }
        });

        Thread.sleep(10000);


    }

}
