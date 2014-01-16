package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.NetworkInfo;
import org.kevoree.NetworkProperty;
import org.kevoree.api.handler.UUIDModel;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.api.helper.CloudModelHelper;
import org.kevoree.library.cloud.lxc.LXCNode;
import org.kevoree.library.cloud.lxc.wrapper.utils.IPAddressValidator;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;

import java.util.List;


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
    private boolean starting = true;
    ModelCloner cloner = new DefaultModelCloner();

    private EmptyCallback callback = new EmptyCallback();

    public LxcSupervision(LXCNode lxcHostNode, LxcManager lxcManager) {
        this.lxcHostNode = lxcHostNode;
        this.lxcManager = lxcManager;
    }

    @Override
    public void run() {
        ContainerRoot model;
        if (starting) {
            model = lxcHostNode.modelService.getCurrentModel().getModel();
            ContainerNode nodeElement = model.findNodesByID(lxcHostNode.getNodeName());
            List<String> lxcNodes = lxcManager.getContainers();
            boolean updateIsNeeded = false;
            if (lxcNodes.size() > nodeElement.getHosts().size()) {
                updateIsNeeded = true;
            }
            if (!updateIsNeeded) {
                for (String nodeName : lxcNodes) {
                    if (model.findNodesByID(nodeName).getHost().getName().equals(nodeElement.getName())) {
                        updateIsNeeded = true;
                        break;
                    }
                }
            }
            if (updateIsNeeded) {
                try {
                    model = cloner.clone(lxcHostNode.modelService.getCurrentModel().getModel());
                    UUIDModel uuidModel = lxcHostNode.modelService.getCurrentModel();
                    if (lxcManager.createModelFromSystem(lxcHostNode.getNodeName(), model)) {
                        lxcHostNode.modelService.compareAndSwap(model, uuidModel.getUUID(), callback);
                    }
                } catch (Exception e) {
                    Log.error("Unable to update the model from lxc-ls", e);
                }
            }
            starting = false;
        }

        String script = "";
        for (ContainerNode containerNode : lxcHostNode.modelService.getCurrentModel().getModel().findNodesByID(lxcHostNode.getNodeName()).getHosts()) {
            if (CloudModelHelper.isASubType(containerNode.getTypeDefinition(), "LXCNode")) {
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
                                Log.info("The Container {} has the IP address => {}", containerNode.getName(), ip);
                                script = "network " + containerNode.getName() + ".ip.eth0 " + ip + "\n";
                            }
                        } else {
                            Log.error("The format of the ip is not well defined");
                        }
                    }
                } else {
                    if (containerNode.getStarted()) {
                        Log.warn("The container {} is not running. Trying to start it", containerNode.getName());
                        lxcManager.start_container(containerNode);
                    }
                }
            }
        }

        if (!script.equals("")) {
            try {
                model = cloner.clone(lxcHostNode.modelService.getCurrentModel().getModel());
                UUIDModel uuidModel = lxcHostNode.modelService.getCurrentModel();
                KevScriptEngine engine = new KevScriptEngine();
                engine.execute(script, model);
                lxcHostNode.modelService.compareAndSwap(model, uuidModel.getUUID(), callback);
            } catch (Exception e) {
                Log.error("Unable to update the model with the IP addresses", e);
            }
        }
    }

    private class EmptyCallback implements UpdateCallback {

        @Override
        public void run(Boolean aBoolean) {

        }
    }
}
