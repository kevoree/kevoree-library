package org.kevoree.library.wrapper.port;

import org.kevoree.Channel;
import org.kevoree.annotation.Input;
import org.kevoree.api.Callback;
import org.kevoree.api.Port;
import org.kevoree.library.wrapper.ComponentWrapper;
import org.kevoree.log.Log;
import org.kevoree.reflect.ReflectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:52
 */

public class InputPort implements Port {

    private Object componentInstance;
    private org.kevoree.Port port;
    private ComponentWrapper componentWrapper;
    private Method method = null;
    private int paramSize = 0;
    private List<StoredCall> pending = new ArrayList<>();

    public InputPort(Object targetObj, org.kevoree.Port port, ComponentWrapper componentWrapper) {
        this.componentInstance = targetObj;
        this.port = port;
        this.componentWrapper = componentWrapper;

        method = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), port.getName(), Input.class);
        if (method != null) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            paramSize = method.getParameterTypes().length;
            if (paramSize > 1) {
                throw new RuntimeException("@Input port method for \""+port.getName()+"\" in " +
                        componentWrapper.getModelElement().getName() + " has too many parameters (current: " + paramSize + ", expected: 0|1)");
            }
        } else {
            throw new RuntimeException("Unable to find @Input port method for \""+port.getName()+"\" in " +
                    componentWrapper.getModelElement().getName());
        }
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        this.port.getBindings().forEach(binding -> {
            if (binding.getHub() != null) {
                channels.add(binding.getHub());
            }
        });
        return channels;
    }

    @Override
    public String getPath() {
        return port.path();
    }

    @Override
    public void send(String payload) {
        this.send(payload, null);
    }

    public void send(String payload, Callback callback) {
        try {
            if (componentWrapper.isStarted()) {
                Log.debug("{} -> {}.{}", payload, componentWrapper.getModelElement().getName(), method.getName());
                Object result;
                ClassLoader chanCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(componentInstance.getClass().getClassLoader());
                if (paramSize == 0) {
                    result = method.invoke(componentInstance);
                } else {
                    result = method.invoke(componentInstance, payload);
                }
                Thread.currentThread().setContextClassLoader(chanCL);

                if (callback != null) {
                    String stringResult;
                    if (result != null) {
                        stringResult = result.toString();
                    } else {
                        stringResult = null;
                    }
                    CallbackInvoker.call(stringResult, callback, port.path());
                }
            } else {
                Log.debug("{} -> {}.{} (queued)", payload, componentWrapper.getModelElement().getName(), method.getName());
                // store internalDispatch somewhere
                pending.add(new StoredCall(payload, callback));
            }
        } catch (Throwable e) {
            Log.error("Input port \"{}\" method threw an exception", e.getCause(), port.path());
        }
    }

    public void processPending() {
        if (!pending.isEmpty()) {
            Thread t = new Thread(() -> {
                for (StoredCall c : pending) {
                    send(c.payload, c.callback);
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

        public Object getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public Callback getCallback() {
            return callback;
        }

        public void setCallback(Callback callback) {
            this.callback = callback;
        }
    }
}