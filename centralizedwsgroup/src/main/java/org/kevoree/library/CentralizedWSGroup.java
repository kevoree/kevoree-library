package org.kevoree.library;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.service.KevScriptService;
import org.kevoree.service.ModelService;
import org.kevoree.library.client.ClientAdapter;
import org.kevoree.library.server.ServerAdapter;
import org.kevoree.library.util.GroupHelper;

import java.net.URI;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by leiko on 16/11/15.
 */
@GroupType(version = 2, description = "WebSocket group based on a centralized architecture that only sends partial model to connected clients")
public class CentralizedWSGroup {

    private static final Pattern MASTER_NET = Pattern.compile("^([a-z0-9A-Z]+)\\.([a-z0-9A-Z]+)$");

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private KevScriptService kevsService;

    @Param(optional = false, fragmentDependent = true)
    private boolean isMaster = false;

    @Param(optional = false, fragmentDependent = true)
    private String masterNet = "lo.ipv4";

    @Param(optional = false)
    private int port = 9000;

    @Param
    private String onDisconnect;

    @Param(optional = false)
    private boolean reduceModel = true;

    private FragmentFacade facade;
    private int previousPort;

    @Start
    public void start() {
        previousPort = port;
        if (this.isMaster) {
            this.facade = new ServerAdapter(this);
        } else {
            this.facade = new ClientAdapter(this);
        }
        this.facade.start();
    }

    @Stop
    public void stop() {
        if (this.facade != null) {
            this.facade.close();
        }
    }

    @Update
    public void update() {
        if (previousPort != port) {
            ServerAdapter serverAdapter = (ServerAdapter) this.facade;
            serverAdapter.broadcast(this.getModelService().getProposedModel());
            stop();
            start();
        }
    }

    public boolean isMaster() {
        return isMaster;
    }

    public String getMasterNet() {
        return masterNet;
    }

    public int getPort() {
        return port;
    }

    public String getOnDisconnect() {
        return onDisconnect;
    }

    public boolean isReduceModel() {
        return reduceModel;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public KevScriptService getKevsService() {
        return kevsService;
    }

    public Context getContext() {
        return context;
    }

    public ContainerRoot getModel() {
        ContainerRoot model = getModelService().getProposedModel();
        if (model == null) {
            model = getModelService().getCurrentModel();
        }
        return model;
    }

    public String getName() {
        return getContext().getInstanceName();
    }

    public URI getURI() {
        String uri;
        if (this.getPort() == 443) {
            uri = "wss://";
        } else {
            uri = "ws://";
        }
        Matcher masterNetMatcher = MASTER_NET.matcher(this.getMasterNet());
        if (masterNetMatcher.matches()) {
            String masterNetName = masterNetMatcher.group(1);
            String masterNetValueName = masterNetMatcher.group(2);
            ContainerRoot currentModel = this.getModel();
            Group group = (Group) currentModel.findByPath(this.getContext().getPath());
            ContainerNode masterNode = GroupHelper.findMasterNode(group);
            if (masterNode != null) {
                HashMap<String, HashMap<String, String>> nets = GroupHelper.findMasterNets(group, masterNode);
                HashMap<String, String> masterNetValues = nets.get(masterNetName);
                if (masterNetValues != null) {
                    String networkValue = masterNetValues.get(masterNetValueName);
                    if (networkValue != null) {
                        return URI.create(uri + networkValue + ":" + this.getPort());
                    } else {
                        throw new KevoreeParamException("Unable to find network value name \""+masterNetValueName+"\" for master node \""+masterNode.getName()+"\"");
                    }
                } else {
                    throw new Error("Unable to find network \""+masterNetName+"\" for master node \""+masterNode.getName()+"\"");
                }
            } else {
                throw new KevoreeParamException("No master node found. Did you at least set one \"isMaster\" to \"true\"?");
            }
        } else {
            throw new KevoreeParamException(this.getContext().getNodeName(), "masterNet",
                    "must comply with /^([a-z0-9A-Z]+)\\\\.([a-z0-9A-Z]+)$/");
        }
    }
}
