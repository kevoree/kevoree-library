package org.kevoree.library.command;

import org.kevoree.ContainerNode;
import org.kevoree.MBinding;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.ChannelWrapper;
import org.kevoree.library.wrapper.ComponentWrapper;
import org.kevoree.library.wrapper.port.InputPort;
import org.kevoree.library.wrapper.port.OutputPort;
import org.kevoree.library.wrapper.port.RemoteInputPort;
import org.kevoree.library.wrapper.port.RemoteOutputPort;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

public class AddBinding implements AdaptationCommand {

    private MBinding binding;
    private String nodeName;
    private InstanceRegistry registry;

    public AddBinding(MBinding binding, String nodeName, InstanceRegistry registry) {
        this.binding = binding;
        this.nodeName = nodeName;
        this.registry = registry;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        Object chanObj = registry.get(binding.getHub());

        if (chanObj != null) {
            ContainerNode portNode = (ContainerNode) binding.getPort().eContainer().eContainer();
            boolean isInput = binding.getPort().getRefInParent().equals("provided");
            ChannelWrapper chan = (ChannelWrapper) chanObj;

            if (portNode.getName().equals(nodeName)) {
                // local port: component is hosted on this node
                Object compObj = registry.get(binding.getPort().eContainer());
                if (compObj != null) {
                    ComponentWrapper comp = (ComponentWrapper) compObj;
                    if (isInput) {
                        // local port is an input
                        InputPort input = comp.getInputs().get(binding.getPort().path());
                        if (input == null) {
                            throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                                    binding.getPort().path() + " (input port instance not found)");
                        } else {
                            chan.getContext().internalAddLocalInput(input);
                            Log.debug("Binding added {} <-> {}", input.getPath(), chan.getContext().getPath());
                        }
                    } else {
                        // local port is an output
                        OutputPort output = comp.getOutputs().get(binding.getPort().path());
                        if (output == null) {
                            throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                                    binding.getPort().path() + " (output port instance not found)");
                        } else {
                            output.internalAddChannel(chan);
                            chan.getContext().internalAddLocalOutput(output);
                            Log.debug("Binding added {} <-> {}", output.getPath(), chan.getContext().getPath());
                        }
                    }

                } else {
                    throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                            binding.getPort().path() + " (component instance not found)");
                }
            } else {
                // remote port: component is not hosted on this node
                if (isInput) {
                    // remote port is an input
                    chan.getContext().internalAddRemoteInput(new RemoteInputPort(binding.getPort()));
                } else {
                    // remote port is an output
                    chan.getContext().internalAddRemoteOutput(new RemoteOutputPort(binding.getPort()));
                }
            }
        } else {
            throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                    binding.getPort().path() + " (channel instance not found)");
        }
//
//
//
//        String portName = binding.getPort().getPortTypeRef().getName();
//        OutputPort output = comp.getOutputs().get(portName);
//        InputPort input = comp.getInputs().get(portName);
//
//        if (output == null && input == null) {
//            throw new KevoreeAdaptationException("Port instance "+portName+" not found in component");
//        }
//
//        if (output != null) {
//            output.addChannelWrapper(binding, chan);
//            Log.debug("Bind output {} {}", binding.getPort().path(), binding.getHub().path());
//            return;
//        }
//
//        chan.getContext().internalAddPort(output);
////            ChannelWrapper chan
//        ((ChannelWrapper) chanObj).getContext().getBoundPorts().put(input.getPath(), input);
//        Log.debug("Bind input {} {}", binding.getPort().path(), binding.getHub().path());
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new RemoveBinding(binding, nodeName, registry).execute();
    }

    private void assertNotNull(Object obj, String type) throws KevoreeAdaptationException {
        if (obj == null) {
            throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                    binding.getPort().path() + " ("+type+" instance unavailable)");
        }
    }

    private void assertType(Object obj, Class clazz) throws KevoreeAdaptationException {
        if (!clazz.isInstance(obj)) {
            throw new KevoreeAdaptationException("Unable to bind channel " + binding.getHub().path() + " to port " +
                    binding.getPort().path() + " (incompatible type)");
        }
    }

    @Override
    public KMFContainer getElement() {
        return binding;
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.ADD_BINDING;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + binding.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "AddBinding       " + binding.getHub().path() + " <-> " + binding.getPort().path();
    }
}
