package org.kevoree.library.java.command;

import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.MBinding;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;
import org.kevoree.log.Log;

public class AddBindingCommand implements PrimitiveCommand {

    private MBinding c;
    private String nodeName;
    private ModelRegistry registry;

    public AddBindingCommand(MBinding c, String nodeName, ModelRegistry registry) {
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
    }

   public void undo() {
        new RemoveBindingCommand(c, nodeName, registry).execute();
    }


    @Override
    public boolean execute() {
        Object kevoreeChannelFound =  registry.lookup(c.getHub());
        Object kevoreeComponentFound = registry.lookup(c.getPort().eContainer());
        if (kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound instanceof ComponentWrapper && kevoreeChannelFound instanceof ChannelWrapper) {
            String portName = c.getPort().getPortTypeRef().getName();
            RequiredPortImpl foundNeedPort = ((ComponentWrapper) kevoreeComponentFound).getRequiredPorts().get(portName);
            ProvidedPortImpl foundHostedPort = ((ComponentWrapper) kevoreeComponentFound).getProvidedPorts().get(portName);
            if (foundNeedPort == null && foundHostedPort == null) {
                Log.info("Port instance {} not found in component", portName);
                return false;
            }
            if (foundNeedPort != null) {
                foundNeedPort.getDelegate().add((ChannelWrapper) kevoreeChannelFound);
                return true;
            }
            if (foundHostedPort != null) {
                //Seems useless
                //ComponentInstance component = (ComponentInstance) c.getPort().eContainer();
                ((ChannelWrapper) kevoreeChannelFound).getContext().getPortsBinded().put("$component/$portName", foundHostedPort);
                return true;
            }
            return false;
        } else {
            Log.error("Error while apply binding , channelFound=${kevoreeChannelFound}, componentFound=${kevoreeComponentFound}");
            return false;
        }
    }

    public String toString() {
        return "AddBindingCommand ${c.hub?.name} <-> ${(c.port?.eContainer() as? ComponentInstance)?.name}";
    }
}
