package org.kevoree.library.wrapper;

import org.kevoree.api.*;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 *
 * Created by duke on 9/26/14.
 */
public class ChannelWrapper extends KInstanceWrapper {

    private ArrayList<Message> pendingMsgs = new ArrayList<>();
    private ChannelContextImpl context;

    public ChannelWrapper(ChannelContextImpl context) {
        this.context = context;
    }

    @Override
    public void startInstance() throws InvocationTargetException {
        super.startInstance();
        processPending();
    }

    public ChannelContextImpl getContext() {
        return context;
    }

    public void internalDispatch(String payload, Callback callback) {
        String connectedInputs = context.getInputs().stream()
                .map(Port::getPath)
                .reduce((p, n) -> p + ", " + n)
                .orElse("");

        if (isStarted()) {
            ChannelDispatch channel = (ChannelDispatch) getTargetObj();
            Log.debug(" {} -> {} -> [{}] (dispatch)", getModelElement().getName(), payload, connectedInputs);
            try {
                ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(channel.getClass().getClassLoader());
                channel.dispatch(payload, callback);
                Thread.currentThread().setContextClassLoader(previousCL);
            } catch (Throwable e) {
                Log.error("Channel \"{}\" dispatch threw an exception", e.getCause(), getModelElement().getName());
                e.printStackTrace();
            }
        } else {
            Log.debug(" {} -> {} -> [{}] (queued)", getModelElement().getName(), payload, connectedInputs);
            pendingMsgs.add(new Message(payload, callback));
        }
    }

    private void processPending() {
        if (!pendingMsgs.isEmpty()) {
            Thread t = new Thread(() -> {
                for (Message c : pendingMsgs) {
                    internalDispatch(c.payload, c.callback);
                }
                pendingMsgs.clear();
            });
            t.start();
        }
    }

    private final class Message {
        private String payload;
        private Callback callback;

        private Message(String payload, Callback callback) {
            this.payload = payload;
            this.callback = callback;
        }
    }
}
