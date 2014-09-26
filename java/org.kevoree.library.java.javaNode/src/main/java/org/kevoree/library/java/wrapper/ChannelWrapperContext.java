package org.kevoree.library.java.wrapper;

import org.kevoree.Channel;
import org.kevoree.ContainerNode;
import org.kevoree.MBinding;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ModelService;
import org.kevoree.api.Port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by duke on 9/26/14.
 */
public class ChannelWrapperContext implements ChannelContext {

    private String channelPath;
    private String localNodePath;

    public ChannelWrapperContext(String channelPath, String localNodePath, ModelService modelService) {
        this.channelPath = channelPath;
        this.localNodePath = localNodePath;
        this.modelService = modelService;
    }

    private ModelService modelService;

    @Override
    public List<Port> getLocalPorts() {
        return new ArrayList(portsBinded.values());
    }

    HashMap<String, Port> portsBinded = new HashMap<String, Port>();

    @Override
    public List<String> getRemotePortPaths() {
        ArrayList<String> result = new ArrayList<String>();
        Channel channel = (Channel) modelService.getCurrentModel().getModel().findByPath(channelPath);
        if (channel != null) {
            for (MBinding binding : channel.getBindings()) {

                ContainerNode parentNode = (ContainerNode) binding.getPort().eContainer().eContainer();
                if (parentNode != null) {
                    if (!parentNode.getName().equals(localNodePath)) {
                        org.kevoree.Port finalPort = binding.getPort();
                        if (finalPort != null) {
                            result.add(finalPort.path());
                        }
                    }
                }
            }
        }
        return result;
    }
}
