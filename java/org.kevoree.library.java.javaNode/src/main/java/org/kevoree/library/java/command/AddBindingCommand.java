package org.kevoree.library.java.command;

import org.kevoree.ComponentInstance;
import org.kevoree.MBinding;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.ChannelWrapper;
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
        Object chan = registry.lookup(c.getHub());
        Object comp = registry.lookup(c.getPort().eContainer());
        if (chan != null && comp != null && comp instanceof ComponentWrapper && chan instanceof ChannelWrapper) {
            String portName = c.getPort().getPortTypeRef().getName();
            RequiredPortImpl output = ((ComponentWrapper) comp).getRequiredPorts().get(portName);
            ProvidedPortImpl input = ((ComponentWrapper) comp).getProvidedPorts().get(portName);

            if (output == null && input == null) {
                Log.info("Port instance {} not found in component", portName);
                return false;
            }

            if (output != null) {
                output.addChannelWrapper(c, (ChannelWrapper) chan);
                Log.info("Bind {} {}", c.getPort().path(), c.getHub().path());
                return true;
            }

            // Seems useless
            ((ChannelWrapper) chan).getContext().getBoundPorts().put(input.getPath(), input);
            Log.info("Bind {} {}", c.getPort().path(), c.getHub().path());
            return true;
        } else {
            Log.error("Error while apply binding , channelFound=" + chan + ", componentFound=" + comp);
            return false;
        }
    }

    public String toString() {
        String hubName = "";
        if (c.getHub() != null) {
            hubName = c.getHub().getName();
        }
        String compName = "";
        if (c.getPort() != null) {
            compName = ((ComponentInstance) c.getPort().eContainer()).getName();
        }
        return "AddBindingCommand " + hubName + " <-> " + compName;
    }
}
