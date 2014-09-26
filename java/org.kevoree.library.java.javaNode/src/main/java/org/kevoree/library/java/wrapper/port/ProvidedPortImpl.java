package org.kevoree.library.java.wrapper.port;

import org.kevoree.api.Port;
import org.kevoree.api.Callback;

import java.lang.reflect.Method;
import org.kevoree.log.Log;
import org.kevoree.library.java.wrapper.ComponentWrapper;
import java.lang.invoke.MethodHandle;
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
    private String name;
    private String portPath;
    private ComponentWrapper componentWrapper;


    private Method targetMethod = null;
    private int paramSize = 0;
    private MethodHandle methodHandler = null;


    public ProvidedPortImpl(Object targetObj, String name, String portPath, ComponentWrapper componentWrapper) {
        this.targetObj = targetObj;
        this.name = name;
        this.portPath = portPath;
        this.componentWrapper = componentWrapper;

        targetMethod = MethodResolver.resolve(name, targetObj.getClass());
        targetMethod.setAccessible(true);
        if (targetMethod == null) {
            Log.error("Warning Provided port is not binded ... for name " + name);
        } else {
            paramSize = targetMethod.getParameterTypes().length;
        }
        // var mt = MethodType.methodType(targetMethod!!.getReturnType()!!,targetMethod!!.getParameterTypes()!!)
        //  methodHandler = MethodHandles.lookup().findVirtual(targetObj.javaClass, name, mt)
    }

    public int getConnectedBindingsSize(){
        throw new UnsupportedOperationException();
    }

    public void send(Object payload) {
        call(payload, null);
    }

    private List<StoredCall> pending = new ArrayList<StoredCall>();

    private class StoredCall {
        private Object payload;
        private Callback<Object> callback;

        private StoredCall(Object payload, Callback<Object> callback) {
            this.payload = payload;
            this.callback = callback;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        public Callback<Object> getCallback() {
            return callback;
        }

        public void setCallback(Callback<Object> callback) {
            this.callback = callback;
        }
    }

    public void call(Object payload, Callback callback) {
        try {
            if (componentWrapper.isStarted()) {
                Object result = null;
                if (paramSize == 0) {
                    if (methodHandler != null) {
                        result = methodHandler.invokeExact(targetObj);
                    } else {
                        result = targetMethod.invoke(targetObj);
                    }
                } else {
                    if (paramSize == 1) {
                        if (methodHandler != null) {
                            result = methodHandler.invokeExact(targetObj, payload);
                        } else {
                            result = targetMethod.invoke(targetObj, payload);
                        }
                    } else {
                        if (payload.getClass().isArray()) {
                            if (((Object[]) payload).length == paramSize) {
                                if (targetMethod != null) {
                                    result = CallBackCaller.callMethod(targetMethod, targetObj, payload);
                                }
                            } else {
                                callback.onError(new Exception("Non corresponding parameters, " + paramSize + " expected, found " + ((Object[]) payload).length));
                            }
                        } else {
                            callback.onError(new Exception("Non corresponding parameters, " + paramSize + " expected, found : not an array == 1"));
                        }
                    }
                }
                if (callback != null) {
                    CallBackCaller.call(result, callback);
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
                        call(c.payload, c.callback);
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