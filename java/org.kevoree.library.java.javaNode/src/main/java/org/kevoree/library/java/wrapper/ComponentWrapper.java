package org.kevoree.library.java.wrapper;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerRoot;
import org.kevoree.Instance;
import org.kevoree.Port;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duke on 9/26/14.
 */
public class ComponentWrapper extends KInstanceWrapper {

    public HashMap<String, ProvidedPortImpl> getProvidedPorts() {
        return providedPorts;
    }

    public void setProvidedPorts(HashMap<String, ProvidedPortImpl> providedPorts) {
        this.providedPorts = providedPorts;
    }

    public HashMap<String, RequiredPortImpl> getRequiredPorts() {
        return requiredPorts;
    }

    public void setRequiredPorts(HashMap<String, RequiredPortImpl> requiredPorts) {
        this.requiredPorts = requiredPorts;
    }

    public HashMap<String, ProvidedPortImpl> providedPorts = new HashMap<String, ProvidedPortImpl>();
    public HashMap<String, RequiredPortImpl> requiredPorts = new HashMap<String, RequiredPortImpl>();

    public void setModelElement(Instance modelElement) {
        super.setModelElement(modelElement);
        ComponentInstance instance = (ComponentInstance) getModelElement();
        for (Port requiredPort : instance.getRequired()) {
            Field field = recursivelyLookForDeclaredRequiredPort(requiredPort.getPortTypeRef().getName(), getTargetObj().getClass());
            if (field != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                RequiredPortImpl portWrapper = new RequiredPortImpl(requiredPort.path());
                try {
                    field.set(getTargetObj(), portWrapper);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                requiredPorts.put(requiredPort.getPortTypeRef().getName(), portWrapper);
            } else {
                Log.warn("A required Port is defined at the model level but is not available at the implementation level");
            }
        }
        for (Port providedPort : instance.getProvided()) {
            ProvidedPortImpl portWrapper = new ProvidedPortImpl(getTargetObj(), providedPort.getPortTypeRef().getName(), providedPort.path(), this);
            providedPorts.put(providedPort.getPortTypeRef().getName(), portWrapper);
        }

    }

    @Override
    public boolean kInstanceStart(ContainerRoot model) {
        if (!getIsStarted()) {
            try {
                Method met = getResolver().resolve(org.kevoree.annotation.Start.class);
                if (met != null) {
                    met.invoke(getTargetObj());
                }
                setIsStarted(true);
                for (ProvidedPortImpl pp : providedPorts.values()) {
                    pp.processPending();
                }
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Kevoree Component Instance Start Error for {} !", e, getModelElement().internalGetKey());
                setIsStarted(true);//WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false;
            } catch (Exception e) {
                Log.error("Kevoree Component Instance Start Error for {} !", e, getModelElement().internalGetKey());
                setIsStarted(true); //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false;
            }
        } else {
            Log.error("{} already started !", getModelElement().internalGetKey());
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
                Log.error("Kevoree Component Instance Stop Error !", e.getCause());
                return false;

            } catch (Exception e) {
                Log.error("Kevoree Component Instance Stop Error !", e);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void create() {
        for (RequiredPortImpl p: this.getRequiredPorts().values()) {
            System.out.println(p.getPath());
        }
    }

    @Override
    public void destroy() {

    }

    private Field recursivelyLookForDeclaredRequiredPort(String name, Class javaClass) {
        try {
            return javaClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (javaClass.getSuperclass() != null) {
                return recursivelyLookForDeclaredRequiredPort(name, javaClass.getSuperclass());
            } else {
                return null;
            }
        }
    }

    private String buildPortBean(String bean, String portName) {
        CharSequence packName = bean.subSequence(0, bean.lastIndexOf("."));
        CharSequence clazzName = bean.subSequence(bean.lastIndexOf(".") + 1, bean.length());
        return packName.toString() + ".kevgen." + clazzName + "PORT" + portName;
    }


}
