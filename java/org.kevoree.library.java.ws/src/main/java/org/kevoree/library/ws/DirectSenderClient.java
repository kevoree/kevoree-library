package org.kevoree.library.ws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.kevoree.ContainerRoot;
import org.kevoree.serializer.JSONModelSerializer;
import java.net.URI;

/**
 * Created by duke on 6/2/14.
 */
public class DirectSenderClient {

    private static JSONModelSerializer saver = new JSONModelSerializer();

    public static boolean create(String ip, String port, final ContainerRoot model) {
        final WebSocketClient[] client = new WebSocketClient[1];
        URI uri = URI.create("ws://" + ip + ":" + port);
        client[0] = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                client[0].send(saver.serialize(model));
            }

            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };
        try {
            return client[0].connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
