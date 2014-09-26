package org.kevoree.library.java.wrapper;

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

    public KInstanceWrapper wrap(KMFContainer modelElement, Object newBeanInstance, ThreadGroup tg, BootstrapService bs, ModelService modelService) {
        if (modelElement instanceof ComponentInstance) {
            return new ComponentWrapper(modelElement, newBeanInstance, nodeName, tg, bs);
        }
        if (modelElement instanceof Group) {
            return new GroupWrapper(modelElement, newBeanInstance, nodeName, tg, bs);
        }
        if (modelElement instanceof Channel) {
            return new ChannelWrapper(modelElement, newBeanInstance, nodeName, tg, bs, modelService);
        }
        if (modelElement instanceof ContainerNode) {
            return new NodeWrapper(modelElement, newBeanInstance, nodeName, tg, bs);
        }
        throw new Exception("Unknow instance type " + modelElement.metaClassName());
    }

}
