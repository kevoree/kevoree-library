package org.kevoree.library.cloud.lxc;

import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.api.Context;
import org.kevoree.library.cloud.lxc.wrapper.*;
import org.kevoree.library.defaultNodeTypes.JavaNode;
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
public class LXCNode extends JavaNode {

    @KevoreeInject
    protected Context context;

    public static final long CREATE_CLONE_TIMEOUT = 180000l;      // todo add dico
    private LxcManager lxcManager;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private CleanerContainerBackups cleaner;
    private CreateBaseContainer createBaseContainer;
    private LxcSupervision supervision;

//    @Param(optional = true, defaultValue = "1024")
//    public int limit_ram;

    @Param(optional = true, defaultValue = "ubuntu")
    private String initialTemplate;

    @Override
    public void startNode() {
        lxcManager = new LxcManager(initialTemplate);
        super.startNode();
        cleaner = new CleanerContainerBackups(this, lxcManager);    // in charge of remove the backup models
        createBaseContainer = new CreateBaseContainer(this, lxcManager);     // in charge of create the base container
        supervision = new LxcSupervision(this, lxcManager); // in charge of checking that the model is applied

        // schedule the tasks
        executor.schedule(createBaseContainer, 10, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(cleaner, 1, 1, TimeUnit.DAYS);
        executor.scheduleAtFixedRate(supervision, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new LXCWrapperFactory(nodeName, lxcManager);
    }

    public String getNodeName() {
        return context.getInstanceName();
    }
}
