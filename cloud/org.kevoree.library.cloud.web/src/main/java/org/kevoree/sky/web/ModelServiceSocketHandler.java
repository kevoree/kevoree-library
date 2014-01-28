package org.kevoree.sky.web;


import org.kevoree.ContainerRoot;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.serializer.JSONModelSerializer;
import org.kevoree.trace.DefaultTraceSequence;
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

    public ModelServiceSocketHandler(ModelService _modelService) {
        modelService = _modelService;
        modelService.registerModelListener(this);
    }

    public void destroy() {
        modelService.unregisterModelListener(this);
    }

    private void sendModel(WebSocketConnection connection) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            connection.send("model=" + outputStream.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.error("Unable to send the current model", e);
        }
    }

    public void onOpen(WebSocketConnection connection) {
        connections.add(connection);
        sendModel(connection);
    }

    public void onClose(WebSocketConnection connection) {
        connections.remove(connection);
    }

    public void onMessage(final WebSocketConnection connection, String message) {
        Log.trace("Receiving request: {}", message);
        try {
            //JSONObject jsonReader = new JSONObject(message);
            //if (jsonReader.get("diff") != null) {
                connection.send("{\"event\":\"update\"}");
                final TraceSequence sequence = new DefaultTraceSequence().populateFromString(message);
                modelService.submitSequence(sequence, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        if (applied) {
                            connection.send("{\"event\":\"done\"}");
                        } else {
                            connection.send("{\"event\":\"error\"}");
                        }
                        Log.info("model updated : {}", applied);
                    }
                });
            //}
        } catch (Exception e) {
            Log.warn("unable to manage the type of message: {}", e, message);
        }
    }

    public void broadcastMessage() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            String payload = "model=" + outputStream.toString("UTF-8");
            for (WebSocketConnection con : connections) {
                con.send(payload);
            }
        } catch (UnsupportedEncodingException e) {
            Log.error("Unable to send the current model", e);
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
        broadcastMessage();
    }

    @Override
    public void preRollback(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
    }

    @Override
    public void postRollback(ContainerRoot containerRoot, ContainerRoot containerRoot2) {
    }

}
