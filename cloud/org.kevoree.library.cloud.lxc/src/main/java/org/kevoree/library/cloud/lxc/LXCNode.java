package org.kevoree.library.cloud.lxc;

import org.kevoree.annotation.NodeType;
import org.kevoree.library.cloud.lxc.wrapper.LXCWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:03
 */

@NodeType
public class LXCNode extends JavaNode {

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LXCWrapperFactory(nodeName);
    }
}
