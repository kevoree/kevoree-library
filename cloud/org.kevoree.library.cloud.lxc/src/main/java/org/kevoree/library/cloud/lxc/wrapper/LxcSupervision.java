package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.NetworkInfo;
import org.kevoree.Value;
import org.kevoree.library.LXCNode;
import org.kevoree.library.cloud.lxc.wrapper.utils.IPAddressValidator;
import org.kevoree.log.Log;

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

    public LxcSupervision(LXCNode lxcHostNode, LxcManager lxcManager) {
        this.lxcHostNode = lxcHostNode;
        this.lxcManager = lxcManager;
    }

    @Override
    public void run() {
        ContainerRoot model;
        model = lxcHostNode.modelService.getCurrentModel().getModel();

        List<String> lxcNodes = lxcManager.getContainers();
        for (String nodeName : lxcNodes) {
            if (model.findNodesByID(nodeName) != null) {

                break;
            }
        }
        String script = createModelFromSystem(model);
        if (script != null) {
            lxcHostNode.modelService.submitScript(script, null);
        }
        script = manageIPAddresses(model);
        if (script != null) {
            lxcHostNode.modelService.submitScript(script, null);
        }
    }

    public String createModelFromSystem(ContainerRoot model) {
        StringBuilder script = new StringBuilder();
        if (lxcManager.getContainers().size() > 0) {
            ContainerNode parentNode = model.findNodesByID(lxcHostNode.getNodeName());
            for (String node_child_id : lxcManager.getContainers()) {
                if (!node_child_id.equals(lxcManager.clone_id) && parentNode.findHostsByID(node_child_id) == null) {
                    script.append("add ").append(parentNode.getName()).append(".").append(node_child_id).append(" : LXCNode\n");
                    if (lxcManager.isRunning(node_child_id)) {
                        script.append("set ").append(node_child_id).append(".started = 'true'\n");
                    } else {
                        script.append("set ").append(node_child_id).append(".started = 'false'\n");
                    }
                }
            }
        }
        if (script.length() > 0) {
            return script.toString();
        } else {
            return null;
        }
    }

    private String manageIPAddresses(ContainerRoot model) {
        Log.debug("Trying to update ip addresses for already known containers");
        String script = "";
        for (ContainerNode containerNode : model.findNodesByID(lxcHostNode.getNodeName()).getHosts()) {
            if (lxcManager.isRunning(containerNode.getName())) {
                String ip = LxcManager.getIP(containerNode.getName());
                if (ip != null) {
                    if (ipvalidator.validate(ip)) {
                        Boolean found = false;
                        for (NetworkInfo n : containerNode.getNetworkInformation()) {
                            for (Value p : n.getValues()) {
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
            return script;
        }
        return null;
    }
}
