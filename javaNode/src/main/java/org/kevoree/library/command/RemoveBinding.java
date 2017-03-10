package org.kevoree.library.command;

import org.kevoree.ContainerNode;
import org.kevoree.MBinding;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.ChannelWrapper;
import org.kevoree.library.wrapper.ComponentWrapper;
import org.kevoree.library.wrapper.port.OutputPort;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

public class RemoveBinding extends AbstractAdaptationCommand {

    private MBinding binding;
    private String nodeName;
    private InstanceRegistry registry;

    public RemoveBinding(MBinding binding, String nodeName, InstanceRegistry registry) {
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
                        chan.getContext().internalRemovePort(binding.getPort().path());
                        Log.debug("Binding removed {} <-> {}", binding.getPort().path(), chan.getContext().getPath());
                    } else {
                        // local port is an output
                        OutputPort output = comp.getOutputs().get(binding.getPort().path());
                        if (output != null) {
                            output.internalRemoveChannel(chan);
                            Log.debug("Binding removed {} <-> {}", output.getPath(), chan.getContext().getPath());
                        }
                    }

                } else {
                    throw new KevoreeAdaptationException("Unable to unbind channel " + binding.getHub().path() + " to port " +
                            binding.getPort().path() + " (component instance not found)");
                }
            } else {
                // remote port: component is not hosted on this node
                chan.getContext().internalRemovePort(binding.getPort().path());
            }
        } else {
            throw new KevoreeAdaptationException("Unable to unbind channel " + binding.getHub().path() + " to port " +
                    binding.getPort().path() + " (channel instance not found)");
        }


//        ChannelWrapper chan = (ChannelWrapper) registry.get(binding.getHub());
//        ComponentWrapper comp = (ComponentWrapper) registry.get(binding.getPort().eContainer());
//
//        if (chan != null && comp != null && comp instanceof ComponentWrapper && chan instanceof ChannelWrapper) {
//            ComponentInstance compInstance = (ComponentInstance) binding.getPort().eContainer();
//            Port input = compInstance.findProvidedByID(binding.getPort().getName());
//            if (input != null) {
//                // port is an input (provided)
//                chan.getContext().getBoundPorts().remove(input.getName());
//                Log.debug("Unbind input {} <-> {}", input.path(), chan.getModelElement().path());
//            } else {
//                // port is an output (required)
//                OutputPort outputWrapper = comp.getOutputs().get(binding.getPort().getName());
//                outputWrapper.removeChannelWrapper(binding);
//                Log.debug("Unbind output {} <-> {}", binding.getPort().path(), chan.getModelElement().path());
//            }
//
//            // retrieve every bindings related to this binding chan
//            for (MBinding b: binding.getHub().getBindings()) {
//                if (b != binding) {
//                    compInstance = (ComponentInstance) b.getPort().eContainer();
//                    input = compInstance.findProvidedByID(b.getPort().getName());
//                    if (input != null) {
//                        // port is an input
//                        chan.getContext().getBoundPorts().remove(input.getName());
//                        Log.debug("Unbind input {} <-> {}", input.path(), chan.getModelElement().path());
//                    }
//                }
//            }
//
//
//            // OLD ALGO:
////                String portName = binding.getPort().getPortTypeRef().getName();
////                OutputPort foundNeedPort = ((ComponentWrapper) comp).getOutputs().get(portName);
////                InputPort foundHostedPort = ((ComponentWrapper) comp).getInputs().get(portName);
////                if (foundNeedPort == null && foundHostedPort == null) {
////                    Log.info("Port instance not found in component");
////                    return false;
////                }
////                if (foundNeedPort != null) {
////                    foundNeedPort.removeChannelWrapper(binding);
////                    return true;
////                }
////
////                ((ChannelWrapper) chan).getContext().getBoundPorts().remove(foundHostedPort.getPath());
//        } else {
//            throw new KevoreeAdaptationException("Unable to remove binding (chan=" + chan + ", comp=" + comp + ")");
//        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new AddBinding(binding, nodeName, registry).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.REMOVE_BINDING;
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
        return "RemoveBinding    " + binding.getHub().path() + " <-> " + binding.getPort().path();
    }

    @Override
    public KMFContainer getElement() {
        return binding;
    }
}
