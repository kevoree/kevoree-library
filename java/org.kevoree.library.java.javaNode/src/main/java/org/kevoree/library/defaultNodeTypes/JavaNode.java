package org.kevoree.library.defaultNodeTypes;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.framework.KevoreeXmiHelper;
import org.kevoree.library.defaultNodeTypes.planning.KevoreeKompareBean;
import org.kevoree.log.Log;
import org.kevoreeadaptation.AdaptationModel;
import org.kevoreeadaptation.AdaptationPrimitive;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * @author ffouquet
 */
@NodeType
@Library(name = "Java")
public class JavaNode implements ModelListener, org.kevoree.api.NodeType {

    protected KevoreeKompareBean kompareBean = null;
    protected CommandMapper mapper = null;
    protected Map<String, Object> registry;

    @KevoreeInject
    public ModelService modelService = null;

    @KevoreeInject
    public BootstrapService bootstrapService = null;

    @Param(optional = false)
    public Boolean debug;

    @Start
    @Override
    public void startNode() {
        Log.debug("Starting node type of {}", this);
        Log.info("Debug mode : {} ", debug);
        registry = new HashMap<String, Object>();
        mapper = new CommandMapper(registry);
        preTime = System.currentTimeMillis();
        modelService.registerModelListener(this);
        kompareBean = new KevoreeKompareBean(registry);
        updateNode();

    }

    @Stop
    @Override
    public void stopNode() {
        Log.debug("Stopping node type of {}", modelService.getNodeName());
        modelService.unregisterModelListener(this);
        kompareBean = null;
        mapper = null;
        //Cleanup the local runtime
        registry.clear();
    }

    @Update
    @Override
    public void updateNode() {
    }

    @Override
    public AdaptationModel plan(ContainerRoot current, ContainerRoot target) {
        return kompareBean.plan(current, target, modelService.getNodeName());
    }

    @Override
    public org.kevoree.api.PrimitiveCommand getPrimitive(AdaptationPrimitive adaptationPrimitive) {
        return mapper.buildPrimitiveCommand(adaptationPrimitive, modelService.getNodeName(), bootstrapService);
    }

    private Long preTime = 0l;

    @Override
    public boolean preUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        preTime = System.currentTimeMillis();
        Log.info("JavaNode received a new Model to apply...");
        return true;
    }

    @Override
    public boolean initUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        mapper.doEnd();
        Log.info("JavaNode Update completed in {} ms", (System.currentTimeMillis() - preTime) + "");
        return true;
    }

    @Override
    public void modelUpdated() {
    }

    @Override
    public void preRollback(ContainerRoot containerRoot, ContainerRoot containerRoot1) {
        Log.warn("JavaSENode is aborting last update...");
    }

    @Override
    public void postRollback(ContainerRoot containerRoot, ContainerRoot containerRoot1) {
        Log.warn("JavaSENode update aborted in {} ms", (System.currentTimeMillis() - preTime) + "");
        try {
            File preModel = File.createTempFile("pre" + System.currentTimeMillis(), "pre");
            File afterModel = File.createTempFile("post" + System.currentTimeMillis(), "post");
            KevoreeXmiHelper.instance$.save(preModel.getAbsolutePath(), containerRoot);
            KevoreeXmiHelper.instance$.save(afterModel.getAbsolutePath(), containerRoot1);
            Log.error("PreModel->" + preModel.getAbsolutePath());
            Log.error("PostModel->" + afterModel.getAbsolutePath());
        } catch (Exception e) {
            Log.error("Error while saving debug model", e);
        }

    }
}
