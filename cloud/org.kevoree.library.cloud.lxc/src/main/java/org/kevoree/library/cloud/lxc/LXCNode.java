package org.kevoree.library.cloud.lxc;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.cloud.api.PlatformJavaNode;
import org.kevoree.library.cloud.lxc.wrapper.*;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:03
 */

@NodeType
public class LXCNode extends PlatformJavaNode {

    @KevoreeInject
    protected Context context;

    private LxcManager lxcManager;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private CleanerContainerBackups cleaner;
    private LxcSupervision supervision;

    @Param(optional = true, defaultValue = "ubuntu")
    private String initialTemplate;

    @Start
    public void startLXCNode() throws Exception {
        // TODO check if the node is run on top of a linux OS and maybe also check if LXC is well configured (?)

        lxcManager = new LxcManager(initialTemplate, new LxcRessourceConstraintManager());
        super.startNode();
        cleaner = new CleanerContainerBackups(this, lxcManager);    // in charge of remove the backup models
        supervision = new LxcSupervision(this, lxcManager);         // in charge of checking that the model is applied
        try {
            lxcManager.install();
            lxcManager.createClone();
        } catch (Exception e) {
            throw new Exception("Unable to configure current system to manage LXC containers", e);
        }
        // schedule the tasks
        executor.scheduleAtFixedRate(cleaner, 1, 1, TimeUnit.DAYS);
        executor.scheduleAtFixedRate(supervision, 10, 10, TimeUnit.SECONDS);
    }

    @Stop
    public void stopLXCNode() {
        executor.shutdown();
        super.stopNode();
    }

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LXCWrapperFactory(nodeName, lxcManager);
    }

    public String getNodeName() {
        return context.getInstanceName();
    }
}
