package org.kevoree.library.java.command;

import org.kevoree.Channel;
import org.kevoree.ContainerNode;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ModelService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.library.java.KevoreeThreadGroup;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.wrapper.ChannelWrapperContext;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.log.Log;


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 17:53
 */
public class AddInstance implements PrimitiveCommand, Runnable {

    private WrapperFactory wrapperFactory;
    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private BootstrapService bs;
    private ModelService modelService;

    private ThreadGroup tg = null;
    private boolean resultSub = false;

    public AddInstance(WrapperFactory wrapperFactory, Instance c, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public boolean execute() {
        Thread subThread = null;
        try {
            tg = new KevoreeThreadGroup("kev/" + c.path());
            subThread = new Thread(tg, this);
            subThread.start();
            subThread.join();
            return resultSub;
        } catch (Throwable e) {
            if (subThread != null) {
                try {
                    //subThread.stop(); //kill sub thread
                    subThread.interrupt();
                } catch (Throwable t) {
                    //ignore killing thread
                }
            }
            Log.error("Could not add the instance {}:{}", c.getName(), c.getTypeDefinition().getName(), e);
            return false;
        }
    }

    public void undo() {
        new RemoveInstance(wrapperFactory, c, nodeName, registry, bs, modelService).execute();
    }

    public void run() {
        try {
            FlexyClassLoader fcl = bs.installTypeDefinition(c);

            Thread.currentThread().setContextClassLoader(fcl);
            Thread.currentThread().setName("KevoreeInstance_" + c.path());

            KInstanceWrapper instanceWrapper;
            if (c instanceof ContainerNode) {
                instanceWrapper = wrapperFactory.wrap(c, this/* nodeInstance is useless because launched as external process */, tg, bs, modelService);
                instanceWrapper.setKcl(fcl);
            } else if (c instanceof Channel) {
                ContainerNode platformNode = modelService.getPendingModel().findNodesByID(nodeName);
                ChannelWrapperContext ctx = new ChannelWrapperContext(c.path(), modelService);
                bs.registerService(ChannelContext.class, ctx);
                Object newBeanInstance = bs.createInstance(c, fcl);
                bs.unregisterService(ChannelContext.class);
                instanceWrapper = wrapperFactory.wrap(c, newBeanInstance, tg, bs, modelService);
                ((ChannelWrapper) instanceWrapper).setContext(ctx);
                instanceWrapper.setKcl(fcl);
                bs.injectDictionary(c, newBeanInstance, true);
            } else {
                Object newBeanInstance = bs.createInstance(c, fcl);
                instanceWrapper = wrapperFactory.wrap(c, newBeanInstance, tg, bs, modelService);
                instanceWrapper.setKcl(fcl);
                bs.injectDictionary(c, newBeanInstance, true);
            }
            registry.register(c, instanceWrapper);
            instanceWrapper.create();
            resultSub = true;
            Thread.currentThread().setContextClassLoader(null);
            Log.info("Add instance {}", c.path());
        } catch(Throwable e) {
            Log.error("Error while adding instance {}", e, c.getName());
            resultSub = false;
        }
    }


    public String toString() {
        return "AddInstance " + c.getName();
    }
}
