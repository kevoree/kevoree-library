package org.kevoree.library;

import org.kevoree.*;
import org.kevoree.annotation.*;
import org.kevoree.annotation.NodeType;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.adaptation.AdaptationType;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.library.java.KevoreeThreadGroup;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.command.*;
import org.kevoree.library.java.network.UDPWrapper;
import org.kevoree.library.java.planning.KevoreeKompareBean;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.log.Log;

import java.net.SocketException;


/**
 * @author ffouquet
 */
@NodeType(version=1)
public class JavaNode implements ModelListener, org.kevoree.api.NodeType {

    protected KevoreeKompareBean kompareBean = null;
    protected ModelRegistry modelRegistry = new ModelRegistry();

    @KevoreeInject
    private ModelService modelService = null;

    @KevoreeInject
    private BootstrapService bootstrapService = null;

    @KevoreeInject
    private Context context = null;

    public void setLog(String log) {
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

    @Param(defaultValue = "INFO")
    public String log = "INFO";

    /**
     * java VM properties used when this node is hosted by a parent node (parent can be also the watchdog)
     */
    @Param(optional = true)
    public String jvmArgs;

    protected WrapperFactory wrapperFactory = null;

    private UDPWrapper adminSrv;
    private Thread adminReader;
    private Long preTime = 0L;

    @Start
    public void startNode() throws Exception {
        preTime = System.currentTimeMillis();
        modelService.registerModelListener(this);
        kompareBean = new KevoreeKompareBean(modelRegistry);
        wrapperFactory = createWrapperFactory(modelService.getNodeName());
        ContainerNode thisNode = modelService.getPendingModel().findNodesByID(context.getNodeName());
        KevoreeThreadGroup tg = new KevoreeThreadGroup("kev/" + context.getPath());
        KInstanceWrapper nodeWrapper = wrapperFactory.wrap(thisNode, this, tg, bootstrapService, modelService);
        nodeWrapper.setStarted(true);
        modelRegistry.register(thisNode, nodeWrapper);

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
        Log.info("Stopping {}", context.getPath());
        modelService.unregisterModelListener(this);
        kompareBean = null;
        modelRegistry.clear();
    }

    @Update
    public void update() {
        this.setLog(this.log);
    }

    @Override
    public AdaptationModel plan(ContainerRoot current, ContainerRoot target) {
        return kompareBean.plan(current, target, modelService.getNodeName());
    }

    @Override
    public PrimitiveCommand getPrimitive(AdaptationPrimitive adaptationPrimitive) {
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
            return new UpdateCallMethod((Instance) adaptationPrimitive.getRef(), modelRegistry);
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
            Object[] values = (Object[]) adaptationPrimitive.getRef();
            return new AddDeployUnit((Instance) values[0], (DeployUnit) values[1], bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.RemoveDeployUnit.name())) {
            Object[] values = (Object[]) adaptationPrimitive.getRef();
            return new RemoveDeployUnit((Instance) values[0], (DeployUnit) values[1], bootstrapService);
        }
        if (pTypeName.equals(AdaptationType.AddInstance.name())) {
            return new AddInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        if (pTypeName.equals(AdaptationType.RemoveInstance.name())) {
            return new RemoveInstance(wrapperFactory, (Instance) adaptationPrimitive.getRef(), nodeName, modelRegistry, bootstrapService, modelService);
        }
        return null;
    }

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
