package org.kevoree.library.java.wrapper;

import org.kevoree.*;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.pmodeling.api.KMFContainer;

/**
 * Created by duke on 9/26/14.
 */
public class WrapperFactory {

    public WrapperFactory(String nodeName) {
        this.nodeName = nodeName;
    }

    private String nodeName;

    public KInstanceWrapper wrap(KMFContainer modelElement, Object newBeanInstance, ThreadGroup tg, BootstrapService bs, ModelService modelService) throws Exception {

        KInstanceWrapper wrapper = null;
        if (modelElement instanceof ComponentInstance) {
            wrapper = new ComponentWrapper();
        }
        if (modelElement instanceof Group) {
            wrapper = new GroupWrapper();
        }
        if (modelElement instanceof Channel) {
            wrapper = new ChannelWrapper();
        }
        if (modelElement instanceof ContainerNode) {
            wrapper = new NodeWrapper();
        }
        if (wrapper != null) {
            wrapper.setModelService(modelService);
            wrapper.setBs(bs);
            wrapper.setIsStarted(false);
            wrapper.setTg(tg);
            wrapper.setNodeName(nodeName);
            wrapper.setTargetObj(newBeanInstance);
            wrapper.setModelElement((Instance) modelElement);
            return wrapper;
        } else {
            throw new Exception("Unknow instance type " + modelElement.metaClassName());
        }
    }

}
