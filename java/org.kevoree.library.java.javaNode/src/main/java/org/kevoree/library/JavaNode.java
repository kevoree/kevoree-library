package org.kevoree.library;

import java.net.SocketException;

import org.kevoree.ContainerRoot;
import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.MBinding;
import org.kevoree.Value;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.adaptation.AdaptationType;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.command.AddBindingCommand;
import org.kevoree.library.java.command.AddDeployUnit;
import org.kevoree.library.java.command.AddInstance;
import org.kevoree.library.java.command.LinkDeployUnit;
import org.kevoree.library.java.command.RemoveBindingCommand;
import org.kevoree.library.java.command.RemoveDeployUnit;
import org.kevoree.library.java.command.RemoveInstance;
import org.kevoree.library.java.command.StartStopInstance;
import org.kevoree.library.java.command.UpdateCallMethod;
import org.kevoree.library.java.command.UpdateDictionary;
import org.kevoree.library.java.network.UDPWrapper;
import org.kevoree.library.java.planning.KevoreeKompareBean;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.log.Log;


/**
 * @author ffouquet
 */
@NodeType(version=1)
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

    @Param(optional = true, defaultValue = "INFO")
    public String lol;


    /**
     * java VM properties used when this node is hosted by a parent node (parent can be also the watchdog)
     */
    @Param(optional = true)
    public String jvmArgs;

//    @Param
//    public String test;

    protected WrapperFactory wrapperFactory = null;

    UDPWrapper adminSrv;
    Thread adminReader;

    @Start
    public void startNode() {
        Log.info("Starting node type of {}", modelService.getNodeName());
        preTime = System.currentTimeMillis();
        modelService.registerModelListener(this);
        kompareBean = new KevoreeKompareBean(modelRegistry);
        wrapperFactory = createWrapperFactory(modelService.getNodeName());
        modelRegistry.registerFromPath(context.getPath(), this);
        if (System.getProperty("node.admin") != null) {
            try {
                adminSrv = new UDPWrapper(Integer.parseInt(System.getProperty("node.admin").toString()));
            } catch (SocketException e) {
                Log.error("", e);
            }
            adminReader = new Thread(adminSrv);
            adminReader.start();
        }
    }

    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new WrapperFactory(nodeName);
    }

    @Stop
    public void stopNode() {
        if (adminReader != null) {
            try {
                adminReader.interrupt();
            } catch (Exception e) {
                Log.error("Error while stopping admin thread JavaNode ", e);
            }
        }
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
        if (pTypeName.equals(AdaptationType.UpdateDictionaryInstance.name())) {
            Object[] values = (Object[]) adaptationPrimitive.getRef();
            if (values.length > 2) {
                return new UpdateDictionary((Instance) values[0], (Value) values[1], (Boolean) values[2], nodeName, modelRegistry, bootstrapService, modelService);
            } else {
                return new UpdateDictionary((Instance) values[0], (Value) values[1], false, nodeName, modelRegistry, bootstrapService, modelService);
            }
        }
        if (pTypeName.equals(AdaptationType.UpdateCallMethod.name())) {
            return new UpdateCallMethod((Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.StartInstance.name())) {
            return new StartStopInstance((Instance) adaptationPrimitive.getRef(), nodeName, true, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.StopInstance.name())) {
            return new StartStopInstance((Instance) adaptationPrimitive.getRef(), nodeName, false, modelRegistry, bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.AddBinding.name())) {
            return new AddBindingCommand((MBinding) adaptationPrimitive.getRef(), nodeName, modelRegistry);
        }
        if (pTypeName.equals(AdaptationType.RemoveBinding.name())) {
            return new RemoveBindingCommand((MBinding) adaptationPrimitive.getRef(), nodeName, modelRegistry);
        }
        if (pTypeName.equals(AdaptationType.AddDeployUnit.name())) {
            return new AddDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.LinkDeployUnit.name())) {
            return new LinkDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService, modelRegistry);
        }
        if (pTypeName.equals(AdaptationType.RemoveDeployUnit.name())) {
            return new RemoveDeployUnit((DeployUnit) adaptationPrimitive.getRef(), bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.AddInstance.name())) {
            return new AddInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        if (pTypeName.equals(AdaptationType.RemoveInstance.name())) {
            return new RemoveInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
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
    public void modelUpdated() {}

    @Override
    public void preRollback(UpdateContext context) {
        Log.warn("JavaSENode is aborting last update...");
    }

    @Override
    public void postRollback(UpdateContext context) {
        Log.warn("JavaSENode update aborted in {} ms", (System.currentTimeMillis() - preTime) + "");
    }
}
