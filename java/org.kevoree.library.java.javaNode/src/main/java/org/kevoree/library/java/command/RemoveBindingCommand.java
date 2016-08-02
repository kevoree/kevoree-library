package org.kevoree.library.java.command;

import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.MBinding;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.ComponentInstance;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
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
            Object kevoreeChannelFound = registry.lookup(c.getHub());
            Object kevoreeComponentFound = registry.lookup(c.getPort().eContainer());
            if(kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound instanceof ComponentWrapper && kevoreeChannelFound instanceof ChannelWrapper){
                String portName = c.getPort().getPortTypeRef().getName();
                RequiredPortImpl foundNeedPort = ((ComponentWrapper) kevoreeComponentFound).getRequiredPorts().get(portName);
                ProvidedPortImpl foundHostedPort = ((ComponentWrapper) kevoreeComponentFound).getProvidedPorts().get(portName);
                if(foundNeedPort == null && foundHostedPort == null){
                    Log.info("Port instance not found in component");
                    return false;
                }
                if (foundNeedPort != null) {
                    foundNeedPort.removeChannelWrapper(c);
                    return true;
                }

                ((ChannelWrapper) kevoreeChannelFound).getContext().getBoundPorts().remove(foundHostedPort.getPath());
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
