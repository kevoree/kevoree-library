package org.kevoree.library;

import org.kevoree.*;
import org.kevoree.annotation.*;
import org.kevoree.annotation.NodeType;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.command.*;
import org.kevoree.library.java.planning.JavaPrimitive;
import org.kevoree.library.java.planning.KevoreeKompareBean;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.log.Log;


/**
 * @author ffouquet
 */
@NodeType
public class JavaNode implements ModelListener, org.kevoree.api.NodeType {

    protected KevoreeKompareBean kompareBean = null;
    protected ModelRegistry modelRegistry = new ModelRegistry();

    @KevoreeInject
    public ModelService modelService = null;

    @KevoreeInject
    public BootstrapService bootstrapService = null;

    @KevoreeInject
    Context context;

    public void setLog(String log) {
        this.log = log;
        if ("DEBUG".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_DEBUG);
        } else if ("WARN".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_WARN);
        } else if ("INFO".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_INFO);
        } else if ("ERROR".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_ERROR);
        } else if ("TRACE".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_TRACE);
        } else if ("NONE".equalsIgnoreCase(log)) {
            Log.set(Log.LEVEL_NONE);
        }
        Log.info("JavaNode, changing LOG level to {}", this.log);
    }

    @Param(optional = true, defaultValue = "INFO")
    public String log;


    /**
     * java VM properties used when this node is hosted by a parent node (parent can be also the watchdog)
     */
    @Param(optional = true)
    public String jvmArgs;

    protected WrapperFactory wrapperFactory = null;

    @Start
    public void startNode() {
        Log.info("Starting node type of {}", modelService.getNodeName());
        preTime = System.currentTimeMillis();
        modelService.registerModelListener(this);
        kompareBean = new KevoreeKompareBean(modelRegistry);
        wrapperFactory = createWrapperFactory(modelService.getNodeName());
        modelRegistry.registerFromPath(context.getPath(), this);
    }

    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new WrapperFactory(nodeName);
    }

    @Stop
    public void stopNode() {
        Log.info("Stopping node type of {}", modelService.getNodeName());
        modelService.unregisterModelListener(this);
        kompareBean = null;
        modelRegistry.clear();
    }

    @Override
    public AdaptationModel plan(ContainerRoot current, ContainerRoot target) {
        return kompareBean.plan(current, target, modelService.getNodeName());
    }

    @Override
    public org.kevoree.api.PrimitiveCommand getPrimitive(AdaptationPrimitive adaptationPrimitive) {
        String pTypeName = adaptationPrimitive.getPrimitiveType();
        String nodeName = modelService.getNodeName();
        if (pTypeName.equals(JavaPrimitive.UpdateDictionaryInstance.name())) {
            Object[] values = (Object[]) adaptationPrimitive.getRef();
            return new UpdateDictionary((Instance) values[0], (Value) values[1], nodeName, modelRegistry, bootstrapService, modelService);
        }
        if (pTypeName.equals(JavaPrimitive.UpdateCallMethod.name())) {
            return new UpdateCallMethod((Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(JavaPrimitive.StartInstance.name())) {
            return new StartStopInstance((Instance) adaptationPrimitive.getRef(), nodeName, true, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(JavaPrimitive.StopInstance.name())) {
            return new StartStopInstance((Instance) adaptationPrimitive.getRef(), nodeName, false, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(JavaPrimitive.AddBinding.name())) {
            return new AddBindingCommand((MBinding) adaptationPrimitive.getRef(), nodeName, modelRegistry);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveBinding.name())) {
            return new RemoveBindingCommand((MBinding) adaptationPrimitive.getRef(), nodeName, modelRegistry);
        }
        if (pTypeName.equals(JavaPrimitive.AddDeployUnit.name())) {
            return new AddDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService);
        }
        if (pTypeName.equals(JavaPrimitive.LinkDeployUnit.name())) {
            return new LinkDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService, modelRegistry);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveDeployUnit.name())) {
            RemoveDeployUnit res = new RemoveDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService);
            return res;
        }
        if (pTypeName.equals(JavaPrimitive.AddInstance.name())) {
            return new AddInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveInstance.name())) {
            return new RemoveInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        if (pTypeName.equals(JavaPrimitive.UpgradeInstance.name())) {
            return new UpgradeInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        return null;
    }

    private Long preTime = 0l;

    @Override
    public boolean preUpdate(UpdateContext context) {
        preTime = System.currentTimeMillis();
        Log.info("JavaNode received a new Model to apply from {}", context.getCallerPath());
        return true;
    }

    @Override
    public boolean initUpdate(UpdateContext context) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(UpdateContext context) {
        Log.info("JavaNode Update completed in {} ms", (System.currentTimeMillis() - preTime) + "");
        return true;
    }

    @Override
    public void modelUpdated() {
    }

    @Override
    public void preRollback(UpdateContext context) {
        Log.warn("JavaSENode is aborting last update...");
    }

    @Override
    public void postRollback(UpdateContext context) {
        Log.warn("JavaSENode update aborted in {} ms", (System.currentTimeMillis() - preTime) + "");
    }
}
