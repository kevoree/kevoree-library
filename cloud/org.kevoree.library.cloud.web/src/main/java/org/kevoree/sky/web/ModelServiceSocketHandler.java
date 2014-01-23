package org.kevoree.sky.web;

import org.kevoree.ContainerRoot;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.LockCallBack;
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
import java.util.UUID;

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


    private RepeatLockCallBack callback = new RepeatLockCallBack();

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
        ContainerRoot model = (ContainerRoot) jsonLoader.loadModelFromString(message).get(0);
        broadcastMessage("event=update");
        int index = 10;
        boolean updateDone;
        while (!(updateDone = applyUpdate(model)) && index > 0) {
            index--;
        }
        if (!updateDone) {
            Log.error("After many tries (10), it is not possible to update the current configuration...");
        }
    }

    private boolean applyUpdate(final ContainerRoot model) {
        callback.initialize(model);
        modelService.acquireLock(callback, 10000l);
        return callback.isUpdateDone();
    }

    private class RepeatLockCallBack implements LockCallBack {
        private boolean updateDone;
        private ContainerRoot model;

        private RepeatLockCallBack() {
        }

        void initialize(ContainerRoot model) {
            updateDone = false;
            this.model = model;
        }

        @Override
        public void run(UUID uuid, Boolean error) {
            if (uuid != null && !error) {
                modelService.compareAndSwap(model, uuid, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        broadcastMessage("event=done");
                        updateDone = true;
                        notify();
                    }
                });
            } else {
                notify();
            }
        }

        public boolean isUpdateDone() {
            try {
                wait(10000);
            } catch (InterruptedException ignored) {
            }
            return updateDone;
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
