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

        KInstanceWrapper wrapper = null;
        if (modelElement instanceof ComponentInstance) {
            return new ComponentWrapper();
        }
        if (modelElement instanceof Group) {
            return new GroupWrapper();
        }
        if (modelElement instanceof Channel) {
            return new ChannelWrapper();
        }
        if (modelElement instanceof ContainerNode) {
            return new NodeWrapper();
        }
        if (wrapper != null) {
            wrapper.setBs(bs);
            wrapper.setIsStarted(false);
            wrapper.setTg(tg);
            wrapper.setNodeName(nodeName);
            wrapper.setTargetObj(newBeanInstance);
            wrapper.setModelElement(modelElement);
        }

        throw new Exception("Unknow instance type " + modelElement.metaClassName());
    }

}
