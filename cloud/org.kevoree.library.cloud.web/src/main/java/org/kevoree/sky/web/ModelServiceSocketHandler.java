package org.kevoree.sky.web;


import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.kevoree.pmodeling.api.trace.TraceSequence;
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
                connection.send("event=update");
                final TraceSequence sequence = new TraceSequence(new DefaultKevoreeFactory()).populateFromString(message);
                modelService.submitSequence(sequence, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        if (applied) {
                            connection.send("event=done");
                        } else {
                            connection.send("event=error");
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
    public boolean preUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean initUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public void modelUpdated() {
        broadcastMessage();
    }

    @Override
    public void preRollback(UpdateContext context) {
    }

    @Override
    public void postRollback(UpdateContext context) {
    }

}
