package org.kevoree.library.wrapper;

import org.kevoree.ComponentInstance;
import org.kevoree.Instance;
import org.kevoree.Port;
import org.kevoree.library.wrapper.port.InputPort;
import org.kevoree.library.wrapper.port.OutputPort;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Objects;

/**
 *
 */
public class ComponentWrapper extends KInstanceWrapper {

    private HashMap<String, InputPort> providedPorts = new HashMap<>();
    private HashMap<String, OutputPort> requiredPorts = new HashMap<>();

    @Override
    public void setModelElement(Instance modelElement) {
        super.setModelElement(modelElement);
        ComponentInstance instance = (ComponentInstance) getModelElement();

        for (Port output : instance.getRequired()) {
            requiredPorts.put(output.path(), new OutputPort(getTargetObj(), output, this));
        }
        for (Port input : instance.getProvided()) {
            providedPorts.put(input.path(), new InputPort(getTargetObj(), input, this));
        }
    }

    @Override
    public void startInstance() throws InvocationTargetException {
        try {
            super.startInstance();
            providedPorts.values()
                    .stream()
                    .filter(Objects::nonNull)
                    .forEachOrdered(InputPort::processPending);
        } catch (InvocationTargetException e) {
            setStarted(true); //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
            throw e;
        }
    }

    public HashMap<String, InputPort> getInputs() {
        return providedPorts;
    }

    public HashMap<String, OutputPort> getOutputs() {
        return requiredPorts;
    }
}
