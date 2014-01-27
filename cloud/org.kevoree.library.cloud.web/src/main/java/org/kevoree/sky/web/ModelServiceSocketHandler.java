package org.kevoree.sky.web;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.kevoree.ContainerRoot;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.serializer.JSONModelSerializer;
import org.kevoree.trace.DefaultTraceSequence;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private Map<WebSocketConnection, TraceSequence> updatesToApply;
    private Map<WebSocketConnection, Integer> updatesRetried;

    private UpdateApplier updateApplier;

    public ModelServiceSocketHandler(ModelService _modelService, long delayWhenNothing, long maxRetry) {
        modelService = _modelService;
        updatesToApply = Collections.synchronizedMap(new HashMap<WebSocketConnection, TraceSequence>());
        updatesRetried = Collections.synchronizedMap(new HashMap<WebSocketConnection, Integer>());
        updateApplier = new UpdateApplier(true, delayWhenNothing, maxRetry, updatesToApply, updatesRetried, modelService, this);
        new Thread(updateApplier).start();
        modelService.registerModelListener(this);
    }

    public void destroy() {
        modelService.unregisterModelListener(this);
        updateApplier.shutdown();
        updatesToApply.clear();
        updatesToApply = null;
        updatesRetried.clear();
        updatesRetried = null;
    }

    private void sendModel(WebSocketConnection connection) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            connection.send(new JSONStringer().object().key("model").value(outputStream.toString("UTF-8")).endObject().toString());
        } catch (UnsupportedEncodingException e) {
            Log.error("Unable to send the current model", e);
        } catch (JSONException e) {
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

    public void onMessage(WebSocketConnection connection, String message) {
        Log.trace("Receiving request: {}", message);
        try {
            JSONObject jsonReader = new JSONObject(message);
            if (jsonReader.get("diff") != null) {
                connection.send(new JSONStringer().object().key("event").value("update").endObject().toString());
                TraceSequence sequence = new DefaultTraceSequence().populateFromString(jsonReader.get("diff").toString());
                TraceSequence previous = updatesToApply.get(connection);
                if (previous != null) {
                    sequence.append(previous);
                }
                updatesToApply.put(connection, sequence);
            }
        } catch (JSONException e) {
            Log.warn("unable to manage the type of message: {}", e, message);
        }
    }

    public void broadcastMessage() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jsonSaver.serializeToStream(modelService.getCurrentModel().getModel(), outputStream);
            for (WebSocketConnection con : connections) {
                con.send(new JSONStringer().object().key("model").value(outputStream.toString("UTF-8")).endObject().toString());
            }
        } catch (UnsupportedEncodingException e) {
            Log.error("Unable to send the current model", e);
        } catch (JSONException e) {
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
