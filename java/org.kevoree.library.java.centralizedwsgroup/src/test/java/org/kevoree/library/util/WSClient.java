package org.kevoree.library.util;

import com.pusher.java_websocket.client.WebSocketClient;
import com.pusher.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 *
 * Created by leiko on 1/16/17.
 */
public class WSClient extends WebSocketClient {

    private OnOpenCallback onOpenCallback;
    private OnMessageCallback onMsgCallback;

    public WSClient(URI serverURI) {
        super(serverURI);
    }

    public void onOpen(OnOpenCallback callback) {
        onOpenCallback = callback;
    }

    public void onMessage(OnMessageCallback callback) {
        onMsgCallback = callback;
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (onOpenCallback != null) {
            onOpenCallback.run();
        }
    }

    @Override
    public void onMessage(String message) {
        if (onMsgCallback != null) {
            onMsgCallback.run(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }

    public interface OnOpenCallback {
        void run();
    }

    public interface OnMessageCallback {
        void run(String msg);
    }
}
