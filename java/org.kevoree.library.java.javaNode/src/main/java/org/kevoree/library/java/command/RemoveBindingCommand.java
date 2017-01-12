package org.kevoree.library.java.command;

import org.kevoree.ComponentInstance;
import org.kevoree.MBinding;
import org.kevoree.Port;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;
import org.kevoree.log.Log;

public class RemoveBindingCommand implements PrimitiveCommand {

    private MBinding c;

    public RemoveBindingCommand(MBinding c, String nodeName, ModelRegistry registry) {
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
    }

    private  String nodeName;
    private ModelRegistry registry;

    public void undo() {
        new AddBindingCommand(c, nodeName, registry).execute();
    }

    public boolean execute() {
        try {
            ChannelWrapper chan = (ChannelWrapper) registry.lookup(c.getHub());
            ComponentWrapper comp = (ComponentWrapper) registry.lookup(c.getPort().eContainer());

            if (chan != null && comp != null && comp instanceof ComponentWrapper && chan instanceof ChannelWrapper) {
                ComponentInstance compInstance = (ComponentInstance) c.getPort().eContainer();
                Port input = compInstance.findProvidedByID(c.getPort().getName());
                if (input != null) {
                    // port is an input (provided)
                    chan.getContext().getBoundPorts().remove(input.getName());
                    Log.info("Unbind input {} <-> {}", input.path(), chan.getModelElement().path());
                } else {
                    // port is an output (required)
                    RequiredPortImpl outputWrapper = comp.getRequiredPorts().get(c.getPort().getName());
                    outputWrapper.removeChannelWrapper(c);
                    Log.info("Unbind output {} <-> {}", c.getPort().path(), chan.getModelElement().path());
                }

                // retrieve every bindings related to this binding chan
                for (MBinding b: c.getHub().getBindings()) {
                    if (b != c) {
                        compInstance = (ComponentInstance) b.getPort().eContainer();
                        input = compInstance.findProvidedByID(b.getPort().getName());
                        if (input != null) {
                            // port is an input
                            chan.getContext().getBoundPorts().remove(input.getName());
                            Log.info("Unbind input {} <-> {}", input.path(), chan.getModelElement().path());
                        }
                    }
                }


                // OLD ALGO:
//                String portName = c.getPort().getPortTypeRef().getName();
//                RequiredPortImpl foundNeedPort = ((ComponentWrapper) comp).getRequiredPorts().get(portName);
//                ProvidedPortImpl foundHostedPort = ((ComponentWrapper) comp).getProvidedPorts().get(portName);
//                if (foundNeedPort == null && foundHostedPort == null) {
//                    Log.info("Port instance not found in component");
//                    return false;
//                }
//                if (foundNeedPort != null) {
//                    foundNeedPort.removeChannelWrapper(c);
//                    return true;
//                }
//
//                ((ChannelWrapper) chan).getContext().getBoundPorts().remove(foundHostedPort.getPath());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("REMOVE BINDING FAILED");
            e.printStackTrace();
        }
        return false;
    }

    public String toString() {
        String s = "RemoveBindingCommand ";
        if(c.getHub() != null) {
            s += c.getHub().getName();
        } else {
            s += " hub:null";
        }
        s += "<->";
        if(c.getPort() != null) {
            s += ((ComponentInstance)c.getPort().eContainer()).getName();
        } else {
            s += " port:null";
        }
        return  s ;
    }

}
