package org.kevoree.library.java.wrapper.port;

import org.kevoree.annotation.Input;
import org.kevoree.api.Callback;
import org.kevoree.api.Port;
import org.kevoree.api.helper.ReflectUtils;
import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.log.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:52
 */

public class ProvidedPortImpl implements Port {

    private Object componentInstance;
    private String portPath;
    private ComponentWrapper componentWrapper;


    private Method method = null;
    private int paramSize = 0;


    public ProvidedPortImpl(Object targetObj, org.kevoree.Port port, ComponentWrapper componentWrapper) {
        this.componentInstance = targetObj;
        this.portPath = port.path();
        this.componentWrapper = componentWrapper;

        method = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), Input.class);
        if (method != null) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            paramSize = method.getParameterTypes().length;
        } else {
            throw new RuntimeException("Unable to find @Input port method for \""+port.getName()+"\" in " +
                    componentWrapper.getModelElement().getName());
        }
    }

    public int getConnectedBindingsSize() {
        throw new UnsupportedOperationException();
    }

    private List<StoredCall> pending = new ArrayList<StoredCall>();

    private class StoredCall {
        private String payload;
        private Callback<Object> callback;

        private StoredCall(String payload, Callback<Object> callback) {
            this.payload = payload;
            this.callback = callback;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public Callback<Object> getCallback() {
            return callback;
        }

        public void setCallback(Callback<Object> callback) {
            this.callback = callback;
        }
    }

    @Override
    public void send(String payload) {
        this.send(payload, null);
    }


    public void send(String payload, Callback callback) {
        try {
            if (componentWrapper.isStarted()) {
                Log.debug("{} -> {}.{}", payload, componentWrapper.getModelElement().getName(), method.getName());
                Object result = null;
                if (paramSize == 0) {
                    ClassLoader chanCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(componentInstance.getClass().getClassLoader());
                    result = method.invoke(componentInstance);
                    Thread.currentThread().setContextClassLoader(chanCL);
                } else {
                    if (paramSize == 1) {
                        ClassLoader chanCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(componentInstance.getClass().getClassLoader());
                        result = method.invoke(componentInstance, payload);
                        Thread.currentThread().setContextClassLoader(chanCL);
                    } else {
                        callback.onError(new Exception("Only one parameter is allowed"));
                    }
                }
                if (callback != null) {
                    String stringResult;
                    if (result != null) {
                        stringResult = result.toString();
                    } else {
                        stringResult = null;
                    }
                    CallBackCaller.call(stringResult, callback, portPath);
                }
            } else {
                Log.debug("{} -> {}.{} (queued)", payload, componentWrapper.getModelElement().getName(), method.getName());
                // store call somewhere
                pending.add(new StoredCall(payload, callback));
            }
        } catch (Throwable e) {
            Log.error("Input port \"{}\" method threw an exception", e, portPath);
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

    public String getPath() {
        return portPath;
    }
}