package org.kevoree.library.ws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.kevoree.log.Log;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leiko on 25/03/14.
 */
public class WSClientQueue {

    private List<String> addresses;
    private int timeout = 2000;
    private WSClientHandler handler;

    public WSClientQueue(List<String> addresses, int timeout, final WSClientHandler clientHandler) {
        this.addresses = addresses;
        this.timeout = timeout;
        this.handler = clientHandler;

        this.connectionLoop();
    }

    private void connectionLoop() {
        List<MyWSClient> clients = new ArrayList<MyWSClient>();
        for (final String addr : addresses) {
            try {
                URI uri = URI.create("ws://"+addr);
                MyWSClient client = new MyWSClient(uri);
                clients.add(client);
                try {
                    Log.info("Trying to connect to centralized server {} ...", client.getURI());
                    client.connectBlocking();
                    if (client.getConnection().isOpen()) {
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception ignore) {}
        }

        try {
            Thread.sleep(timeout);
            for (MyWSClient client : clients) {
                if (client.getConnection().isOpen()) {
                    // we good
                    return;
                }
            }
            connectionLoop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyWSClient extends WebSocketClient {
        private boolean connected = false;

        public MyWSClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            handler.setWebSocket(this);
            this.connected = true;
            handler.onOpen(serverHandshake);
        }

        @Override
        public void onMessage(String s) {
            if (this.connected) {
                handler.onMessage(s);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            if (this.connected) {
                this.connected = false;
                handler.onClose(i, s, b);
                connectionLoop();
            }
        }

        @Override
        public void onError(Exception e) {
            if (this.connected) {
                handler.onError(e);
            }
        }
    }

    /**
     * Test
     * @param args
     */
    public static void main(String[] args) {
        List<String> addresses = new ArrayList<String>();
        addresses.add("127.0.0.1:9090");
        addresses.add("127.0.0.1:9091");
        addresses.add("127.0.0.1:9092");

        WSClientQueue queue = new WSClientQueue(addresses, 5000, new WSClientHandler() {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("OPEN");
            }

            @Override
            public void onMessage(String s) {
                System.out.println("MESSAGE");
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("close "+this.getWebSocket().getConnection().getRemoteSocketAddress());
            }

            @Override
            public void onError(Exception e) {}
        });
    }
}
