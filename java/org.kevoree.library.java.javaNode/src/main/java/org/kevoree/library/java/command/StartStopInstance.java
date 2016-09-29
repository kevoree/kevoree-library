package org.kevoree.library.java.command;

import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;

public class StartStopInstance implements PrimitiveCommand, Runnable {

    private Instance c;
    private String nodeName;
    private boolean start;
    private ModelRegistry registry;
    private BootstrapService bs;
    private boolean result = false;
    private KInstanceWrapper instanceWrapper = null;

    public StartStopInstance(Instance c, String nodeName, boolean start, ModelRegistry registry, BootstrapService bs) {
        this.c = c;
        this.nodeName = nodeName;
        this.start = start;
        this.registry = registry;
        this.bs = bs;
    }

    public void run() {
        Thread.currentThread().setContextClassLoader(instanceWrapper.getKcl());
        ModelRegistry.current.set(registry);
        if (start) {
            Thread.currentThread().setName("KevoreeStartInstance_" + c.path());
            try {
                instanceWrapper.startInstance();
                result = true;
            } catch (InvocationTargetException e) {
                Log.error("Error while starting instance " + c.path(), e);
                result = false;
            }
        } else {
            Thread.currentThread().setName("KevoreeStopInstance_" + c.path());
            try {
                instanceWrapper.stopInstance();
                result = true;
            } catch (InvocationTargetException e) {
                Log.error("Error while stopping instance " + c.path(), e);
                result = false;
            }
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    public void undo() {
        new StartStopInstance(c, nodeName, !start, registry, bs).execute();
    }

    public boolean execute() {
        if (start) {
            Log.info("Starting {}", c.path());
        } else {
            Log.info("Stopping {}", c.path());
        }

        Object ref = registry.lookup(c);
        if (ref != null && ref instanceof KInstanceWrapper) {
            instanceWrapper = (KInstanceWrapper) ref;
            Thread t = new Thread(instanceWrapper.getTg(), this);
            t.start();
            try {
                t.join(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!start) {
                //kill subthread
                Thread[] subThreads = new Thread[instanceWrapper.getTg().activeCount()];
                instanceWrapper.getTg().enumerate(subThreads);
                for (Thread subT : subThreads) {
                    try {
                        if (subT.isAlive()) {
                            subT.interrupt();
                        }
                    } catch (Throwable e) {
                        //ignore
                    }
                }
            }
            //call sub
            return result;
        } else {
            Log.error("Unable to find object instance \""+c.path()+"\" (or incompatible with KInstanceWrapper)");
            return false;
        }
    }

    public String toString() {
        String s = "StartStopInstance " + c.getName();
        if (start) {
            s += " start";
        } else {
            s += " stop";
        }
        return s;
    }

}
