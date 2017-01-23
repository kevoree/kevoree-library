package org.kevoree.library.java.wrapper;

import org.kevoree.api.Callback;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by duke on 9/26/14.
 */
public class ChannelWrapper extends KInstanceWrapper {

    private ArrayList<StoredCall> pending = new ArrayList<StoredCall>();
    private ChannelWrapperContext context;

    @Override
    public void startInstance() throws InvocationTargetException {
        super.startInstance();
        processPending();
    }

    public void setContext(ChannelWrapperContext context) {
        this.context = context;
    }

    public ChannelWrapperContext getContext() {
        return this.context;
    }

    public void call(org.kevoree.api.Callback callback, String payload) {
        Set<String> portPaths = new HashSet<>();
        portPaths.addAll(context.getBoundPorts().keySet());
        portPaths.addAll(context.getRemotePortPaths());
        String connectedInputs = portPaths.stream().reduce((p, n) -> p + ", " + n).orElse("");

        if (isStarted()) {
            ChannelDispatch channel = (ChannelDispatch) getTargetObj();
            Log.debug(" {} -> {} -> [{}] (dispatch)", getModelElement().getName(), payload, connectedInputs);
            try {
                ClassLoader nodeCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(channel.getClass().getClassLoader());
                channel.dispatch(payload, callback);
                Thread.currentThread().setContextClassLoader(nodeCL);
            } catch (Throwable e) {
                Log.error("Channel \"{}\" dispatch threw an exception", e.getCause(), getModelElement().getName());
            }
        } else {
            Log.debug(" {} -> {} -> [{}] (queued)", getModelElement().getName(), payload, connectedInputs);
            pending.add(new StoredCall(payload, callback));
        }
    }

    private void processPending() {
        if (!pending.isEmpty()) {
            Thread t = new Thread(() -> {
                for (StoredCall c : pending) {
                    call(c.getCallback(), c.getPayload());
                }
                pending.clear();
            });
            t.start();
        }
    }

    private class StoredCall {

        private String payload;
        private Callback callback;

        private StoredCall(String payload, Callback callback) {
            this.payload = payload;
            this.callback = callback;
        }

        private String getPayload() {
            return payload;
        }

        private void setPayload(String payload) {
            this.payload = payload;
        }

        private Callback getCallback() {
            return callback;
        }

        private void setCallback(Callback callback) {
            this.callback = callback;
        }
    }
}
