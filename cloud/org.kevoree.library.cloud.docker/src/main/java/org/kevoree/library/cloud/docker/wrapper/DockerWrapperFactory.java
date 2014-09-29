package org.kevoree.library.cloud.docker.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.library.DockerNode;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.pmodeling.api.KMFContainer;

/**
 * Created by duke on 9/29/14.
 */
public class DockerWrapperFactory extends WrapperFactory {

    private DockerNode dockerNode;

    public DockerWrapperFactory(String nodeName,DockerNode dockerNode) {
        super(nodeName);
        this.dockerNode = dockerNode;
    }

    @Override
    public KInstanceWrapper wrap(KMFContainer modelElement, Object newBeanInstance, ThreadGroup tg, BootstrapService bs, ModelService modelService) throws Exception {
        if(modelElement instanceof ContainerNode){
            DockerNodeWrapper w = new DockerNodeWrapper(dockerNode);
            w.setTg(tg);
            w.setBs(bs);
            w.setModelService(modelService);
            w.setTargetObj(newBeanInstance);
            w.setModelElement((Instance) modelElement);
            return w;
        } else {
            return super.wrap(modelElement, newBeanInstance, tg, bs, modelService);
        }
    }
}
