package org.kevoree.library;

import org.kevoree.api.Port;
import redis.clients.jedis.JedisPubSub;

/**
 * Created by mleduc on 02/12/15.
 */
public class RedisSubscriber extends JedisPubSub {

    private final Port port;

    public RedisSubscriber(Port port) {
        this.port = port;
    }

    @Override
    public void onMessage(String channel, String message) {
        port.send(message);
    }

    public String getPath() {
        return port.getPath();
    }
}
