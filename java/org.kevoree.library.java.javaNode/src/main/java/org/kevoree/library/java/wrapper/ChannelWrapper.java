package org.kevoree.library.java.wrapper;

import org.kevoree.ContainerRoot;
import org.kevoree.Instance;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by duke on 9/26/14.
 */
public class ChannelWrapper extends KInstanceWrapper {


    @Override
    public boolean kInstanceStart(ContainerRoot model) {
        if (!getIsStarted()) {
            try {
                Method met = getResolver().resolve(org.kevoree.annotation.Start.class);
                if (met != null) {
                    met.invoke(getTargetObj());
                }
                setIsStarted(true);
                processPending();
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Kevoree Channel Instance Start Error !", e.getCause());
                return false;
            } catch (Exception e) {
                Log.error("Kevoree Channel Instance Start Error !", e);
                return false;
            }
        } else {
            Log.error("Try to start the channel {} while it is already start", getModelElement().getName());
            return false;
        }
    }

    @Override
    public boolean kInstanceStop(ContainerRoot model) {
        if (getIsStarted()) {
            try {
                Method met = getResolver().resolve(org.kevoree.annotation.Stop.class);
                if (met != null) {
                    met.invoke(getTargetObj());
                }
                setIsStarted(false);
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Kevoree Channel Instance Stop Error !", e.getCause());
                return false;
            } catch (Exception e) {
                Log.error("Kevoree Channel Instance Stop Error !", e);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void create() {

    }

    @Override
    public void destroy() {

    }

    public ChannelWrapperContext getContext() {
        return context;
    }

    public void setContext(ChannelWrapperContext context) {
        this.context = context;
    }

    private ChannelWrapperContext context;

    public void setModelElement(Instance modelElement) {
        super.setModelElement(modelElement);
        context = new ChannelWrapperContext(modelElement.path(), getNodeName(), getModelService());
        getBs().injectService(ChannelContext.class, context, getTargetObj());
    }

    private ArrayList<StoredCall> pending = new ArrayList<StoredCall>();

    private class StoredCall {

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        public Callback getCallback() {
            return callback;
        }

        public void setCallback(Callback callback) {
            this.callback = callback;
        }

        private Object payload;
        private Callback callback;

        private StoredCall(Object payload, Callback callback) {
            this.payload = payload;
            this.callback = callback;
        }
    }


    public void call(org.kevoree.api.Callback callback, Object payload) {
        if (getIsStarted()) {
            ((ChannelDispatch) getTargetObj()).dispatch(payload, callback);
        } else {
            pending.add(new StoredCall(payload, callback));
        }
    }

    public void processPending() {
        if (!pending.isEmpty()) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    for (StoredCall c : pending) {
                        call(c.getCallback(), c.getPayload());
                    }
                    pending.clear();
                }
            });
            t.start();
        }
    }

}
