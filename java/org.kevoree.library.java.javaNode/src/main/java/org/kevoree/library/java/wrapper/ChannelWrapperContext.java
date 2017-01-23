package org.kevoree.library.java.wrapper;

import org.kevoree.Channel;
import org.kevoree.MBinding;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ModelService;
import org.kevoree.api.Port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 *
 */
public class ChannelWrapperContext implements ChannelContext {

    private HashMap<String, Port> boundPorts = new HashMap<>();
    private ModelService modelService;
    private String channelPath;

    public ChannelWrapperContext(String channelPath, ModelService modelService) {
        this.channelPath = channelPath;
        this.modelService = modelService;
    }

    @Override
    public List<Port> getLocalPorts() {
        return new ArrayList<>(boundPorts.values());
    }

    @Deprecated
    public HashMap<String, Port> getPortsBinded() {
        return this.getBoundPorts();
    }

    public HashMap<String, Port> getBoundPorts() {
        return boundPorts;
    }

    @Deprecated
    public void setPortsBinded(HashMap<String, Port> portsBinded) {
        this.boundPorts = portsBinded;
    }

    public String getChannelPath() {
        return channelPath;
    }

    @Override
    public List<String> getRemotePortPaths() {
        ArrayList<String> result = new ArrayList<>();
        Channel channel = (Channel) modelService.getCurrentModel().getModel().findByPath(channelPath);
        if (channel != null) {
            for (MBinding binding : channel.getBindings()) {
                org.kevoree.Port port = binding.getPort();
                if (port != null && port.getRefInParent().equals("provided")) {
                    result.add(port.path());
                }
//                ContainerNode parentNode = (ContainerNode) binding.getPort().eContainer().eContainer();
//                if (parentNode != null) {
//                    if (!parentNode.getName().equals(localNodePath)) {
//                        org.kevoree.Port finalPort = binding.getPort();
//                        if (finalPort != null) {
//                            // TODO should we just add input ports here??
//                            result.add(finalPort.path());
//                        }
//                    }
//                }
            }
        }
        return result;
    }
}
