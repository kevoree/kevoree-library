package org.kevoree.library.ws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Created by leiko on 25/03/14.
 */
public abstract class WSClientHandler {

    private WebSocketClient ws;

    public abstract void onOpen(ServerHandshake serverHandshake);
    public abstract void onMessage(String s);
    public abstract void onClose(int i, String s, boolean b);
    public abstract void onError(Exception e);

    public void setWebSocket(WebSocketClient ws) {
        this.ws = ws;
    }

    public WebSocketClient getWebSocket() {
        return this.ws;
    }

    public void send(String s) {
        this.ws.send(s);
    }
}
