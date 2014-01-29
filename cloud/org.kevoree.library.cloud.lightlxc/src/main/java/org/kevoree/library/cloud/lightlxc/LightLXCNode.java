package org.kevoree.library.cloud.lightlxc;

import org.kevoree.annotation.Library;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.library.cloud.api.PlatformNode;
import org.kevoree.library.cloud.docker.wrapper.LightLXCWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

/**
 * Created by duke on 09/12/2013.
 */
@NodeType
@Library(name = "Cloud")
public class LightLXCNode extends JavaNode implements PlatformNode {


    /**
     * Parameter to automatically create a route within the hosted platform between two network interfaces
     */
    @Param()
    String routeditfname;

    /**
     * Parameter to set the network alias
     */
    @Param(defaultValue = "eth1")
    String hostitfname;

    /**
     * Parameter to set the default ip of the container
     */
    @Param(defaultValue = "192.168.1.1")
    String hostitfip;


    /**
     * Parameter to set the default prefix for the containers.
     */
    @Param(defaultValue = "192.168.1.1")
    String containeripbaseaddress;

    /**
     * Parameter to Decide if the node has to create a bridge or if the bridge is managed directly by the hosted platform.
     */
    @Param(defaultValue = "true")
    boolean createBrdge;

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LightLXCWrapperFactory(nodeName,routeditfname, hostitfname,hostitfip,containeripbaseaddress,createBrdge);
    }
}
