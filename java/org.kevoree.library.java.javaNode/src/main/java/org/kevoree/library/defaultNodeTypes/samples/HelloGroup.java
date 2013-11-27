package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.framework.AbstractGroupType;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 16/11/2013
 * Time: 11:54
 */


@GroupType
public class HelloGroup extends AbstractGroupType {

    @Start
    public void start() {
        System.out.println("Hello from group");
    }

    @Stop
    public void stop() {
        System.out.println("Bye from group");
    }

    @Override
    public void push(ContainerRoot model, String targetNodeName) throws Exception {
    }

    @Override
    public ContainerRoot pull(String targetNodeName) throws Exception {
        return null;
    }

    @Override
    public void modelUpdated() {
    }
}
