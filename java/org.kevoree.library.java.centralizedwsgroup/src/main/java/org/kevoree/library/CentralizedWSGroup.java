package org.kevoree.library;

import org.kevoree.ContainerRoot;
import org.kevoree.Instance;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.library.client.Client;
import org.kevoree.library.server.Server;
import org.kevoree.log.Log;

/**
 *
 * Created by leiko on 16/11/15.
 */
@GroupType(version = 1, description = "WebSocket group based on a centralized architecture that only sends partial model to connected clients")
public class CentralizedWSGroup {

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ModelService modelService;

    @Param(optional = false, defaultValue = "false", fragmentDependent = true)
    private boolean isMaster;

    @Param(optional = false, defaultValue = "lo.ipv4", fragmentDependent = true)
    private String masterNet;

    @Param(optional = false, defaultValue = "9000")
    private int port;

    private FragmentFacade facade;

    @Start
    public void start() {
        if (this.isMaster) {
            this.facade = new Server(this);
        } else {
            this.facade = new Client(this);
        }
        this.facade.create();
    }

    @Stop
    public void stop() {
        if (this.facade != null) {
            this.facade.close();
        }
    }

    @Update
    public void update() {
        // TODO
        Log.info("TODO update() in \"{}\"", getName());
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

    public ModelService getModelService() {
        return modelService;
    }

    public Context getContext() {
        return context;
    }

    public ContainerRoot getModel() {
        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }
        return model;
    }

    public String getName() {
        return ((Instance) getModel().findByPath(context.getPath())).getName();
    }
}
