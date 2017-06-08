package org.kevoree.library;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.compare.AdaptationEngine;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.library.wrapper.WrapperFactory;
import org.kevoree.log.Log;
import org.kevoree.service.ModelService;
import org.kevoree.service.RuntimeService;

import java.util.List;


/**
 * @author ffouquet
 */
@NodeType(version=2)
public class JavaNode implements org.kevoree.api.NodeType {

    @KevoreeInject
    private ModelService modelService = null;

    @KevoreeInject
    private RuntimeService runtimeService = null;

    @KevoreeInject
    private Context context = null;

    @Param(defaultValue = "INFO")
    private String log = "INFO";

    private AdaptationEngine kompareBean;
    private InstanceRegistry instanceRegistry = new InstanceRegistry();
    private Long startTime = 0L;

    @Start
    public void start() throws Exception {
        startTime = System.currentTimeMillis();
        WrapperFactory wrapperFactory = new WrapperFactory(context.getNodeName());
        kompareBean = new AdaptationEngine(context.getNodeName(), modelService, runtimeService, instanceRegistry, wrapperFactory);
        ContainerNode thisNode = modelService.getProposedModel().findNodesByID(context.getNodeName());
        KInstanceWrapper nodeWrapper = wrapperFactory.wrap(thisNode, this, runtimeService, modelService);
        nodeWrapper.setStarted(true);
        instanceRegistry.put(thisNode, nodeWrapper);
    }

    @Stop
    public void stop() {
        Log.info("Stopping {}", context.getPath());
        kompareBean = null;
        instanceRegistry.clear();
    }

    @Update
    public void update() {
        this.setLog(this.log);
    }

    @Override
    public List<AdaptationCommand> plan(ContainerRoot current, ContainerRoot target) throws KevoreeAdaptationException {
        return kompareBean.plan(current, target);
    }

    private void setLog(String log) {
        boolean changed = false;
        if ("DEBUG".equalsIgnoreCase(log) && !Log.DEBUG) {
            Log.set(Log.LEVEL_DEBUG);
            changed = true;
        } else if ("WARN".equalsIgnoreCase(log) && !Log.WARN) {
            Log.set(Log.LEVEL_WARN);
            changed = true;
        } else if ("INFO".equalsIgnoreCase(log) && !Log.INFO) {
            Log.set(Log.LEVEL_INFO);
            changed = true;
        } else if ("ERROR".equalsIgnoreCase(log) && !Log.ERROR) {
            Log.set(Log.LEVEL_ERROR);
            changed = true;
        } else if ("TRACE".equalsIgnoreCase(log) && !Log.TRACE) {
            Log.set(Log.LEVEL_TRACE);
            changed = true;
        } else if ("NONE".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_NONE);
            changed = true;
        }
        if (changed) {
            Log.info("Node platform \"{}\" changing LOG level to {}", this.context.getInstanceName(), log);
        }
    }
}
