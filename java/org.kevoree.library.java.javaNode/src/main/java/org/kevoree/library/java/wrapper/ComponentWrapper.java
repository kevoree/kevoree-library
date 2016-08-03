package org.kevoree.library.java.wrapper;

import org.kevoree.ComponentInstance;
import org.kevoree.Instance;
import org.kevoree.Port;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 *
 */
public class ComponentWrapper extends KInstanceWrapper {

    private HashMap<String, ProvidedPortImpl> providedPorts = new HashMap<String, ProvidedPortImpl>();
    private HashMap<String, RequiredPortImpl> requiredPorts = new HashMap<String, RequiredPortImpl>();

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

    public void setModelElement(Instance modelElement) {
        super.setModelElement(modelElement);
        ComponentInstance instance = (ComponentInstance) getModelElement();

        for (Port output : instance.getRequired()) {
            RequiredPortImpl outputPort = new RequiredPortImpl(getTargetObj(), output, this);
            requiredPorts.put(output.getPortTypeRef().getName(), outputPort);
        }
        for (Port input : instance.getProvided()) {
            ProvidedPortImpl portWrapper = new ProvidedPortImpl(getTargetObj(), input, this);
            providedPorts.put(input.getPortTypeRef().getName(), portWrapper);
        }

    }

    @Override
    public void startInstance() throws InvocationTargetException {
        try {
            super.startInstance();
            for (ProvidedPortImpl input : providedPorts.values()) {
                if (input != null) {
                    input.processPending();
                }
            }
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
