package org.kevoree.library.mqtt;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.mqtt.internal.ExServer;

import java.io.IOException;

/**
 * Created by duke on 6/17/14.
 */
@ComponentType
public class MQTTServer {

    ExServer server;

    @KevoreeInject
    Context context;

    @Param(defaultValue = "1883")
    Integer port;

    @Start
    public void start() throws IOException {
        server = new ExServer(context.getNodeName(),context.getInstanceName(),port);
        server.startServer();
    }

    @Stop
    public void stop() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
    }


}
