package org.kevoree.library.cloud.docker;

import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.api.ModelService;
import org.kevoree.library.cloud.docker.wrapper.DockerWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
@NodeType
public class DockerNode extends JavaNode {

    @Param(defaultValue = "0")
    private Integer cpuShares;

    @Param(defaultValue = "512")
    private int memory;

    @Param(defaultValue = "true")
    private boolean startKevboot;

    @Param(defaultValue = "kevoree/watchdog")
    private String image;

    @KevoreeInject
    private ModelService modelService;

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new DockerWrapperFactory(nodeName, modelService);
    }
}