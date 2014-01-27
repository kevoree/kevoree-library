package org.kevoree.sky.web;

import org.json.JSONException;
import org.json.JSONStringer;
import org.kevoree.ContainerRoot;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.webbitserver.WebSocketConnection;

import java.util.Map;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 23/01/14
 * Time: 17:42
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public class UpdateApplier implements Runnable {

    private boolean running;
    private long delayWhenNothing;
    private long maxRetry;
    private Map<WebSocketConnection, TraceSequence> updatesToApply;
    private Map<WebSocketConnection, Integer> updatesRetried;

    private ModelService modelService;
    private DefaultModelCloner cloner;

    private ModelServiceSocketHandler socketModelHandler;

    public UpdateApplier(boolean running, long delayWhenNothing, long maxRetry, Map<WebSocketConnection, TraceSequence> updatesToApply, Map<WebSocketConnection, Integer> updatesRetried, ModelService modelService, ModelServiceSocketHandler socketModelHandler) {
        this.running = running;
        this.delayWhenNothing = delayWhenNothing;
        this.maxRetry = maxRetry;
        this.updatesToApply = updatesToApply;
        this.updatesRetried = updatesRetried;
        this.modelService = modelService;
        this.cloner = new DefaultModelCloner();
        this.socketModelHandler = socketModelHandler;
    }

    public void shutdown() {
        running = false;
        try {
            wait();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        while (running) {
            if (updatesToApply.size() == 0) {
                try {
                    Thread.sleep(delayWhenNothing);
                } catch (InterruptedException ignored) {
                }
            } else {
                final WebSocketConnection connection = updatesToApply.keySet().iterator().next();
                TraceSequence sequence = updatesToApply.remove(connection);

                ContainerRoot model = (ContainerRoot) cloner.clone(modelService.getCurrentModel().getModel());
                if (model != null) {
                    sequence.applyOn(model);
                    modelService.update(model, new SynchronizedUpdateCallBack(connection, sequence));
                }
            }
        }
        while (!running && updatesToApply.size() > 0) {
            final WebSocketConnection connection = updatesToApply.keySet().iterator().next();
            updatesToApply.remove(connection);
            try {
                connection.send( new JSONStringer().object().key("event").value("error").key("message").value("Unable to apply the update. This web page is shutting down.").endObject().toString());
            } catch (JSONException e) {
                Log.warn("Unable to notify a client that its update has not been applied", e);
            }
        }
        notify();
    }


    private class SynchronizedUpdateCallBack implements UpdateCallback {

        private WebSocketConnection connection;
        private TraceSequence sequence;

        private SynchronizedUpdateCallBack(WebSocketConnection connection, TraceSequence sequence) {
            this.connection = connection;
            this.sequence = sequence;
        }

        @Override
        public void run(Boolean applied) {
            if (applied) {
                try {
//                    updatesToApply.remove(id);
                    connection.send( new JSONStringer().object().key("event").value("done").endObject().toString());
                } catch (JSONException e) {
                    Log.error("Unable to send successful event on update", e);
                }
            } else {
                updatesToApply.put(connection, sequence);
                int retry = updatesRetried.get(connection);
                retry--;
                if (retry == 0) {
                    try {
                        connection.send(new JSONStringer().object().key("event").value("error").key("message").value("After " + maxRetry + " the creation of the node(s) has not been applied.").endObject().toString());
                    } catch (JSONException e) {
                        Log.warn("Unable to notify a client that its update has not been applied", e);
                    }
                } else {
                    updatesToApply.put(connection, sequence);
                    updatesRetried.put(connection, retry);
                }
            }
        }
    }
}