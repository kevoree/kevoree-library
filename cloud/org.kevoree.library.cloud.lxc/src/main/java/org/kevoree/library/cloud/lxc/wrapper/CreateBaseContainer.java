package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.api.handler.LockCallBack;
import org.kevoree.library.cloud.lxc.LXCNode;
import org.kevoree.log.Log;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 08/07/13
 * Time: 16:00
 * To change this template use File | Settings | File Templates.
 */
public class CreateBaseContainer implements Runnable {

    private LXCNode lxcHostNode;
    private LxcManager lxcManager;

    public CreateBaseContainer(LXCNode lxcHostNode, LxcManager lxcManager) {
        this.lxcHostNode = lxcHostNode;
        this.lxcManager = lxcManager;
    }

    @Override
    public void run() {
        if (!lxcManager.getContainers().contains(lxcManager.clone_id)) {
            Log.debug("Lock the model to create Kevoree base Container");
            lxcHostNode.modelService.acquireLock(new LockCallBack() {
                @Override
                public void run(UUID uuid, Boolean error) {
                    if (uuid != null && !error) {
                        try {
                            lxcManager.install();
                            lxcManager.createClone();
                        } catch (Exception e) {
                            Log.error("Unable to configure current system to manage LXC containers", e);
                        }
                        Log.debug("Unlock the model to create Kevoree base Container");
                        lxcHostNode.modelService.releaseLock(uuid);
                    } else {
                        Log.error("Cannot lock the model to create the base container used for clone");
                    }
                }
            }, lxcHostNode.getCREATE_CLONE_TIMEOUT());
        }


    }
}
