package org.kevoree.library.java.wrapper;

import org.kevoree.api.Callback;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

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
        String connectedInputs = context.getBoundPorts().keySet().stream().reduce((p, n) -> p + ", " + n).get();
        connectedInputs += context.getRemotePortPaths().stream().reduce((p, n) -> p + ", " + n).get();
        if (isStarted()) {
            ChannelDispatch channel = (ChannelDispatch) getTargetObj();
            Log.debug(" {} -> {} -> [{}] (dispatch)", getModelElement().getName(), payload, connectedInputs);
            try {
//                System.out.println("===> channel wrapper context : " + ((FlexyClassLoader) getClass().getClassLoader()).getKey());
                channel.dispatch(payload, callback);
            } catch (Throwable e) {
                Log.error("Channel \"{}\" dispatch threw an exception", e, getModelElement().getName());
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
