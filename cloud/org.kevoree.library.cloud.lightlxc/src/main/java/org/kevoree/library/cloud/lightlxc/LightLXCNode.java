package org.kevoree.library.cloud.lightlxc;

import org.kevoree.annotation.NodeType;
import org.kevoree.library.cloud.api.PlatformNode;
import org.kevoree.library.cloud.docker.wrapper.LightLXCWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

/**
 * Created by duke on 09/12/2013.
 */
@NodeType
public class LightLXCNode extends JavaNode implements PlatformNode {

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LightLXCWrapperFactory(nodeName);
    }
}
