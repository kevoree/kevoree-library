package org.kevoree.sky.web;

import org.kevoree.ContainerRoot;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.serializer.JSONModelSerializer;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 09/07/13
 * Time: 11:58
 */
public class ModelServiceSocketHandler extends BaseWebSocketHandler implements ModelListener {

    private ModelService modelService = null;
    private ArrayList<WebSocketConnection> connections = new ArrayList<WebSocketConnection>();
    private JSONModelSerializer jsonSaver = new JSONModelSerializer();
    private JSONModelLoader jsonLoader = new JSONModelLoader();

    public ModelServiceSocketHandler(ModelService _modelService) {
        modelService = _modelService;
        modelService.registerModelListener(this);
    }

    public void destroy() {
        modelService.unregisterModelListener(this);
    }

    public void onOpen(WebSocketConnection connection) {
        connections.add(connection);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            connection.send("model=" + outputStream.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.error("Can't send base model", e);
        }
    }

    public void onClose(WebSocketConnection connection) {
        connections.remove(connection);
    }

    public void onMessage(WebSocketConnection connection, String message) {
        try {
            ContainerRoot model = (ContainerRoot) jsonLoader.loadModelFromString(message).get(0);
            broadcastMessage("event=update");
            modelService.update(model, new UpdateCallback() {
                @Override
                public void run(Boolean applied) {
                    broadcastMessage("event=done");
                }
            });
        } catch (Exception e) {
            Log.error("Can't send base model", e);
        }
    }

    public void broadcastMessage(String message) {
        for (WebSocketConnection con : connections) {
            con.send(message);
        }
    }

    @Override
    public boolean preUpdate(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
        return true;
    }

    @Override
    public boolean initUpdate(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
        return true;
    }

    @Override
    public void modelUpdated() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            broadcastMessage("model=" + outputStream.toString("UTF-8"));
        } catch (Exception e) {
            Log.error("Can't send base model", e);
        }
    }

    @Override
    public void preRollback(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
    }

    @Override
    public void postRollback(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
    }
}