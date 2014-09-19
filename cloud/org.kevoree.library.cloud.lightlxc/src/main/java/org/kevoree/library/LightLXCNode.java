package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.library.api.PlatformJavaNode;
import org.kevoree.library.cloud.docker.wrapper.LightLXCWrapperFactory;
import org.kevoree.library.cloud.lightlxc.BridgeService;
import org.kevoree.library.cloud.lightlxc.wrapper.IpModelUpdater;
import org.kevoree.library.java.wrapper.WrapperFactory;

/**
 * Created by duke on 09/12/2013.
 */
@NodeType
public class LightLXCNode extends PlatformJavaNode {

    /**
     * Parameter to automatically create a route within the hosted platform between two network interfaces
     */
    @Param(defaultValue = "eth0")
    String routeditfname;

    /**
     * Parameter to set the network alias
     */
    @Param(defaultValue = "eth0")
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

    @Param(defaultValue = "false")
    boolean freeze;

    /**
     * Parameter to Decide if the node has to create a bridge or if the bridge is managed directly by the hosted platform.
     */
    @Param(defaultValue = "false")
    boolean createBridge;

    @Param(defaultValue = "lxcbr0")
    String bridgeName;

    @Param(defaultValue = "255.255.255.0")
    String networkMask;

    @Param(defaultValue = "98")
    Integer ipStep;

    @Param(defaultValue = "1")
    Integer ipStart;


    @Param(defaultValue = "false")
    boolean sshdStart;

    LightLXCWrapperFactory fact = null;

    @KevoreeInject
    ModelService modelsService;

    @KevoreeInject
    Context context;

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        //nodeName: String,   val hostitfname: String,
        // val hostitfip: String, val containeripbaseaddress: String,
        //        val bridgeName : String,val sshdStart : Boolean, val ip:String,
        //       val gw:String,val netmask:String, val mac:String
        if (updater == null) {
            bservice = new BridgeService(createBridge, hostitfname, context.getNodeName(), bridgeName, networkMask, routeditfname, containeripbaseaddress);
            bservice.start();
            updater = new IpModelUpdater(modelsService);
            modelsService.registerModelListener(updater);
        }
        fact = new LightLXCWrapperFactory(nodeName, hostitfname, hostitfip, containeripbaseaddress, bridgeName, sshdStart, ipStep, ipStart, networkMask, updater);
        return fact;
    }

    @Update
    public void updateNode() {
        modelsService.registerModelListener(updater);
        if (fact != null && fact.getWrap() != null) {
            fact.getWrap().freeze(freeze);
        }
    }

    BridgeService bservice = null;


    IpModelUpdater updater;

    @Start
    public void startNode() {
        //System.err.println("pass par la");

        if (updater == null) {
            bservice = new BridgeService(createBridge, hostitfname, context.getNodeName(), bridgeName, networkMask, routeditfname, containeripbaseaddress);
            bservice.start();
            updater = new IpModelUpdater(modelsService);
            modelsService.registerModelListener(updater);
        }
        super.startNode();

    }

    @Stop
    public void stopNode() {
        bservice.stop();
        modelsService.unregisterModelListener(updater);
        super.stopNode();

    }
}
