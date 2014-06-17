package org.kevoree.library.mqtt;

import org.dna.mqtt.moquette.server.Server;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;

import java.io.IOException;

/**
 * Created by duke on 6/17/14.
 */
@ComponentType
public class MQTTServer {

    Server server;

    @Start
    public void start() throws IOException {
        server = new Server();
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
