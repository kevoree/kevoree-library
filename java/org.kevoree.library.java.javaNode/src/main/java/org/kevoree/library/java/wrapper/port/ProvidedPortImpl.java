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

    private Object targetObj;
    private String portPath;
    private ComponentWrapper componentWrapper;


    private Method targetMethod = null;
    private int paramSize = 0;
    //private MethodHandle methodHandler = null;


    public ProvidedPortImpl(Object targetObj, String name, String portPath, ComponentWrapper componentWrapper) {
        this.targetObj = targetObj;
        this.portPath = portPath;
        this.componentWrapper = componentWrapper;

        targetMethod = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), Input.class);
        if (targetMethod != null) {
            if (!targetMethod.isAccessible()) {
                targetMethod.setAccessible(true);
            }
            paramSize = targetMethod.getParameterTypes().length;
        } else {
            throw new RuntimeException("Unable to find @Input method for port \""+name+"\" in " + componentWrapper.getModelElement().getName());
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
                Object result = null;
                if (paramSize == 0) {
                    //if (methodHandler != null) {
                    //result = methodHandler.invokeExact(targetObj);
                    //} else {
                    result = targetMethod.invoke(targetObj);
                } else {
                    if (paramSize == 1) {
                        result = targetMethod.invoke(targetObj, payload);
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
                //store call somewhere
                pending.add(new StoredCall(payload, callback));
            }
        } catch (Throwable e) {
            Log.error("This is really bad, exception during port call...", e);
        }
    }

    public void processPending() {
        if (!pending.isEmpty()) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    for (StoredCall c : pending) {
                        send(c.payload, c.callback);
                    }
                    pending.clear();
                }
            });
            t.start();
        }
    }

    public String getPath() {
        return portPath;
    }


}