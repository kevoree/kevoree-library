package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.Repository;
import org.kevoree.api.handler.LockCallBack;
import org.kevoree.library.cloud.lxc.LXCNode;
import org.kevoree.log.Log;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        List<String> urls = new ArrayList<String>();
        MavenResolver resolver = new MavenResolver();
        for(Repository repo : lxcHostNode.modelService.getCurrentModel().getModel().getRepositories())
        {
            urls.add(repo.getUrl());
        }
        urls.add("https://oss.sonatype.org/content/groups/public/");//not mandatory but for early release
        File watchdog = resolver.resolve("org.kevoree.watchdog", "org.kevoree.watchdog", "RELEASE", "deb",urls);

        if(watchdog != null && watchdog.exists())
        {
            lxcManager.setWatchdogLocalFile(watchdog);
        }
        else
        {

            Log.error("The LxcManager cannot download the kevoree watchdog");
        }
    }

    @Override
    public void run() {
        Log.info("Lock the model to create Kevoree base Container");
        lxcHostNode.modelService.acquireLock(new LockCallBack() {
            @Override
            public void run(UUID uuid, Boolean locked) {
                try {
                    if (!locked) {
                        lxcManager.install();
                        lxcManager.createClone();

                    } else {
                        Log.error("Cannot lock the model to create the base container used for clone");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }   finally
                {
                    Log.info("Unlock the model to create Kevoree base Container");
                    lxcHostNode.modelService.releaseLock(uuid);
                }
            }
        }, LXCNode.CREATE_CLONE_TIMEOUT);



    }
}
