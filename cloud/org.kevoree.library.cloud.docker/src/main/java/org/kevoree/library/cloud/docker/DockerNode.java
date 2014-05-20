package org.kevoree.library.cloud.docker;

import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Update;
import org.kevoree.api.ModelService;
import org.kevoree.library.cloud.docker.wrapper.DockerWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 10:17
 */
@NodeType
public class DockerNode extends JavaNode {

    @KevoreeInject
    ModelService modelsService;

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        System.out.println("CREATE WRAPPER FACTORY");
        return new DockerWrapperFactory(nodeName);
    }

    @Override
    public void startNode() {
        super.startNode();
        System.out.println("START NODE");
    }

    @Update
    public void updateNode() {
        System.out.println("UPDATE");
    }

    @Override
    public void stopNode() {
        super.stopNode();
        System.out.println("STOP NODE");
    }
}