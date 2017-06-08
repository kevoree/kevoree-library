package org.kevoree.library.wrapper;

import org.kevoree.Instance;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.annotation.Update;
import org.kevoree.service.ModelService;
import org.kevoree.service.RuntimeService;
import org.kevoree.reflect.ReflectUtils;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 *
 */
public abstract class KInstanceWrapper {

    private String nodeName;
    private Instance modelElement;
    private ThreadGroup tg;
    private Boolean isStarted;
    private RuntimeService runtimeService;
    private ClassLoader kcl;
    private Object targetObj;
    private ModelService modelService;
    protected Method startMethod;
    protected Method stopMethod;
    protected Method updateMethod;

    public ModelService getModelService() {
        return modelService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Instance getModelElement() {
        return modelElement;
    }

    public void setModelElement(Instance modelElement) {
        this.modelElement = modelElement;
    }

    public Object getTargetObj() {
        return targetObj;
    }

    public void setTargetObj(Object targetObj) {
        this.targetObj = targetObj;
        this.startMethod = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), Start.class);
        this.stopMethod = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), Stop.class);
        this.updateMethod = ReflectUtils.findMethodWithAnnotation(targetObj.getClass(), Update.class);
    }

    public ClassLoader getClassLoader() {
        return kcl;
    }

    public void setClassLoader(ClassLoader kcl) {
        this.kcl = kcl;
    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public Boolean isStarted() {
        return isStarted;
    }

    public void setStarted(Boolean isStarted) {
        this.isStarted = isStarted;
    }

    public ThreadGroup getThreadGroup() {
        return tg;
    }

    public void setThreadGroup(ThreadGroup tg) {
        this.tg = tg;
    }

    public void startInstance() throws InvocationTargetException {
        if (!isStarted()) {
            if (startMethod != null) {
                startMethod.setAccessible(true);
                try {
                    startMethod.invoke(targetObj);
                    setStarted(true);
                } catch (IllegalAccessException e) {
                    Log.error("Unable to access " + modelElement.path() + " @Start method " + startMethod.getName() + "()");
                }
            } else {
                setStarted(true);
                Log.debug("Instance {} has no @Start method", modelElement.path());
            }
        } else {
            Log.debug("Instance {} is already started", modelElement.path());
        }
    }

    public void stopInstance() throws InvocationTargetException {
        if (isStarted()) {
            if (stopMethod != null) {
                stopMethod.setAccessible(true);
                try {
                    stopMethod.invoke(targetObj);
                    setStarted(false);
                } catch (IllegalAccessException e) {
                    Log.error("Unable to access " + modelElement.path() + " @Stop method " + stopMethod.getName() + "()");
                }
            } else {
                setStarted(false);
                Log.debug("Instance {} has no @Stop method", modelElement.path());
            }
        } else {
            Log.debug("Instance {} is already stopped", modelElement.path());
        }
    }

    public void updateInstance() throws InvocationTargetException {
        if (isStarted()) {
            if (updateMethod != null) {
                updateMethod.setAccessible(true);
                try {
                    updateMethod.invoke(targetObj);
                } catch (IllegalAccessException e) {
                    Log.error("Unable to access " + modelElement.path() + " @Update method " + updateMethod.getName() + "()");
                }
            } else {
                Log.debug("Instance {} has no @Update method", modelElement.path());
            }
        } else {
            Log.debug("Instance {} is not started: won't update", modelElement.path());
        }
    }
}
