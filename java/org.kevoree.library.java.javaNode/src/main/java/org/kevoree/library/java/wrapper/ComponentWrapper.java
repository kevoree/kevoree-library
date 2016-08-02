package org.kevoree.library.java.wrapper;

import org.kevoree.ComponentInstance;
import org.kevoree.Instance;
import org.kevoree.Port;
import org.kevoree.annotation.Output;
import org.kevoree.api.helper.ReflectUtils;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 *
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
            Field field = ReflectUtils.findFieldWithAnnotation(
                    requiredPort.getPortTypeRef().getName(),getTargetObj().getClass(), Output.class);
            if (field != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                RequiredPortImpl portWrapper = new RequiredPortImpl(requiredPort.path());
                try {
                    field.set(getTargetObj(), portWrapper);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unable to set @Output field " + requiredPort.getPortTypeRef().getName() + " implementation", e);
                }
                requiredPorts.put(requiredPort.getPortTypeRef().getName(), portWrapper);
            } else {
                throw new RuntimeException("Unable to find @Output field of type "+ org.kevoree.api.Port.class.getName()+" in \""+requiredPort.getPortTypeRef().getName()+"\" in " + modelElement.getName());
            }
        }
        for (Port providedPort : instance.getProvided()) {
            ProvidedPortImpl portWrapper = new ProvidedPortImpl(getTargetObj(), providedPort.getPortTypeRef().getName(), providedPort.path(), this);
            providedPorts.put(providedPort.getPortTypeRef().getName(), portWrapper);
        }

    }

    @Override
    public void startInstance() throws InvocationTargetException {
        try {
            super.startInstance();
        } catch (InvocationTargetException e) {
            setStarted(true); //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
            throw e;
        }
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
}
