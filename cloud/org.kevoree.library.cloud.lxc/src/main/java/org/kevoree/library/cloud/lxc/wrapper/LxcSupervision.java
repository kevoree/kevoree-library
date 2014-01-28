package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.NetworkInfo;
import org.kevoree.NetworkProperty;
import org.kevoree.api.handler.LockCallBack;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.lxc.LXCNode;
import org.kevoree.library.cloud.lxc.wrapper.utils.IPAddressValidator;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;

import java.util.List;
import java.util.UUID;


/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 25/06/13
 * Time: 17:34
 */
public class LxcSupervision implements Runnable {

    private LXCNode lxcHostNode;
    private LxcManager lxcManager;
    private IPAddressValidator ipvalidator = new IPAddressValidator();
    //    private boolean starting = true;
    ModelCloner cloner = new DefaultModelCloner();

    private EmptyCallback callback = new EmptyCallback();

    public LxcSupervision(LXCNode lxcHostNode, LxcManager lxcManager) {
        this.lxcHostNode = lxcHostNode;
        this.lxcManager = lxcManager;
    }

    @Override
    public void run() {

        lxcHostNode.modelService.acquireLock(new LockCallBack() {
            @Override
            public void run(final UUID uuid, Boolean error) {
                if (uuid != null && !error) {
                    Log.debug("Lock the model to manage supervision");
                    ContainerRoot model;
                    model = lxcHostNode.modelService.getCurrentModel().getModel();
                    ContainerNode nodeElement = model.findNodesByID(lxcHostNode.getNodeName());
                    
                    List<String> lxcNodes = lxcManager.getContainers();
                    boolean updateIsNeeded = false;
                    if (lxcNodes.size() - 1 > nodeElement.getHosts().size()) { // -1 correspond to the basekevoreecontainer which is use to clone
                        updateIsNeeded = true;
                    }
                    if (!updateIsNeeded) {
                        for (String nodeName : lxcNodes) {
                            if (model.findNodesByID(nodeName) != null) {
                                updateIsNeeded = true;
                                break;
                            }
                        }
                    }
                    boolean updateMustBeApplied = false;
                    if (updateIsNeeded) {
                        model = cloner.clone(lxcHostNode.modelService.getCurrentModel().getModel());
                        if (lxcManager.createModelFromSystem(lxcHostNode.getNodeName(), model)) {
                            updateMustBeApplied = true;
                        }
                    } else {
                        Log.trace("Update the model according to existing containers is not needed because there seems not to have unknown container");
                    }

                    callback.initialize(uuid);
                    if (manageIPAddresses(model) || updateMustBeApplied) {
                        lxcHostNode.modelService.compareAndSwap(model, uuid, callback);
                    } else {
                        callback.run(false);
                    }
                }
            }
        }, LXCNode.SUPERVISION_TIMEOUT);
    }

    private boolean manageIPAddresses(ContainerRoot model) {
        Log.debug("Trying to update ip addresses for already known containers");
        String script = "";
        for (ContainerNode containerNode : model.findNodesByID(lxcHostNode.getNodeName()).getHosts()) {
            if (LxcManager.isRunning(containerNode.getName())) {
                String ip = LxcManager.getIP(containerNode.getName());
                if (ip != null) {
                    if (ipvalidator.validate(ip)) {
                        Boolean found = false;
                        for (NetworkInfo n : containerNode.getNetworkInformation()) {
                            for (NetworkProperty p : n.getValues()) {
                                if (ip.equals(p.getValue())) {
                                    found = true;
                                }
                            }
                        }
                        if (!found) {
                            Log.debug("The Container {} has the IP address => {}", containerNode.getName(), ip);
                            script = "network " + containerNode.getName() + ".ip.eth0 " + ip + "\n";
                        }
                    } else {
                        Log.error("The format of the ip is not well defined");
                    }
                }
            } else {
                if (containerNode.getStarted()) {
                    // FIXME Do we really need to start the node ?
                    Log.warn("The container {} is not running while it must be running. Trying to start it", containerNode.getName());
                    lxcManager.startContainer(containerNode);
                }
            }
        }

        if (!"".equals(script)) {
            try {
                KevScriptEngine engine = new KevScriptEngine();
                engine.execute(script, model);
                return true;
            } catch (Exception e) {
                Log.error("Unable to update the model with the IP addresses", e);
            }
        }
        return false;
    }

    private class EmptyCallback implements UpdateCallback {

        private UUID uuid;

        private void initialize(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run(Boolean aBoolean) {
            Log.debug("Unlock model after managing supervision");
            lxcHostNode.modelService.releaseLock(uuid);
        }
    }
}
