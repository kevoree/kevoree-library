package org.kevoree.library.cloud.docker;

import org.kevoree.annotation.NodeType;
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

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new DockerWrapperFactory(nodeName);
    }
}