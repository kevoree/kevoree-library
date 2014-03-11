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

    @Param(defaultValue = "180000")
    long CREATE_CLONE_TIMEOUT;
    @Param(defaultValue = "10000")
    long SUPERVISION_TIMEOUT;

    public long getCREATE_CLONE_TIMEOUT() {
        return CREATE_CLONE_TIMEOUT;
    }

    public long getSUPERVISION_TIMEOUT() {
        return SUPERVISION_TIMEOUT;
    }

    private LxcManager lxcManager;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private CleanerContainerBackups cleaner;
    private CreateBaseContainer createBaseContainer;
    private LxcSupervision supervision;

    @Param(optional = true, defaultValue = "ubuntu")
    private String initialTemplate;

    @Start
    public void startNode() {
        // TODO check if the node is run on top of a linux OS and maybe also check if LXC is well configured (?)

        lxcManager = new LxcManager(initialTemplate, new LxcRessourceConstraintManager());
        super.startNode();
        cleaner = new CleanerContainerBackups(this, lxcManager);    // in charge of remove the backup models
        createBaseContainer = new CreateBaseContainer(this, lxcManager);     // in charge of create the base container
        supervision = new LxcSupervision(this, lxcManager); // in charge of checking that the model is applied

        // schedule the tasks
        executor.schedule(createBaseContainer, 10, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(cleaner, 1, 1, TimeUnit.DAYS);
        executor.scheduleAtFixedRate(supervision, 20, 30, TimeUnit.SECONDS);
    }

    @Stop
    public void stopNode() {
        executor.shutdown();
    }

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LXCWrapperFactory(nodeName, lxcManager);
    }

    public String getNodeName() {
        return context.getInstanceName();
    }
}
