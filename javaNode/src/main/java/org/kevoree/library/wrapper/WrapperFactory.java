package org.kevoree.library.wrapper;

import org.kevoree.*;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelContextImpl;
import org.kevoree.api.ModelService;
import org.kevoree.api.RuntimeService;
import org.kevoree.library.compare.KevoreeThreadGroup;
import org.kevoree.modeling.api.KMFContainer;

/**
 *
 * Created by duke on 9/26/14.
 */
public class WrapperFactory {

    public WrapperFactory(String nodeName) {
        this.nodeName = nodeName;
    }

    private String nodeName;

    public KInstanceWrapper wrap(KMFContainer instance, Object instanceObj, RuntimeService runtimeService,
                                 ModelService modelService)
            throws IllegalArgumentException {

        KInstanceWrapper wrapper = null;
        if (instance instanceof ComponentInstance) {
            wrapper = new ComponentWrapper();
        }
        if (instance instanceof Group) {
            wrapper = new GroupWrapper();
        }
        if (instance instanceof Channel) {
            wrapper = new ChannelWrapper((ChannelContextImpl) runtimeService.getService(ChannelContext.class));
        }
        if (instance instanceof ContainerNode) {
            wrapper = new NodeWrapper();
        }
        if (wrapper != null) {
            wrapper.setModelService(modelService);
            wrapper.setRuntimeService(runtimeService);
            wrapper.setStarted(false);
            wrapper.setThreadGroup(new KevoreeThreadGroup(instance.path()));
            wrapper.setNodeName(nodeName);
            wrapper.setTargetObj(instanceObj);
            wrapper.setModelElement((Instance) instance);
            return wrapper;
        } else {
            throw new IllegalArgumentException("Instance type must be ComponentInstance, Group, Channel or ContainerNode (current: " + instance.metaClassName() + ")");
        }
    }

}
