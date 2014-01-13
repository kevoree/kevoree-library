package org.kevoree.library.ws;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.serializer.JSONModelSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@GroupType
@Library(name = "Java :: Groups")
public class WSGroup extends WebSocketServer {

    @KevoreeInject
    public ModelService modelService;

    @Param(optional = true, fragmentDependent = true, defaultValue = "9000")
    Integer port;
    private WSGroup server = null;

    public WSGroup() throws UnknownHostException {
        super();
    }

    public WSGroup(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    @Start
    public void startWSGroup() throws UnknownHostException {
        server = new WSGroup(port);
        server.modelService = modelService;
        server.start();
        Log.info("WSGroup listen on " + port);
    }

    @Stop
    public void stopWSGroup() throws IOException, InterruptedException {
        server.stop();
    }

    @Update
    public void updateWSGroup() throws IOException, InterruptedException {
        stopWSGroup();
        startWSGroup();
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
    }

    private JSONModelLoader jsonModelLoader = new JSONModelLoader();
    private JSONModelSerializer jsonModelSaver = new JSONModelSerializer();

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        if (s.startsWith("pull") || s.startsWith("get")) {
            //Pull
            String modelReturn = jsonModelSaver.serialize(modelService.getCurrentModel().getModel());
            webSocket.send(modelReturn);
        } else {
            //Push
            try {
                ContainerRoot model = (ContainerRoot) jsonModelLoader.loadModelFromString(s).get(0);
                modelService.update(model, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        System.out.println("WSGroup update result : " + applied);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }
}
