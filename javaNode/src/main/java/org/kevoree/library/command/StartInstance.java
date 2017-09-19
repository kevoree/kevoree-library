package org.kevoree.library.command;

import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

import java.lang.reflect.InvocationTargetException;

/**
 *
 * Created by leiko on 3/7/17.
 */
public class StartInstance implements AdaptationCommand {

    private Instance instance;
    private InstanceRegistry registry;
    private InvocationTargetException error;

    public StartInstance(Instance instance, InstanceRegistry registry) {
        this.instance = instance;
        this.registry = registry;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        KInstanceWrapper instanceWrapper = this.registry.get(instance, KInstanceWrapper.class);

        if (instanceWrapper != null) {
            ThreadGroup threadGroup = instanceWrapper.getThreadGroup();
            if (threadGroup == null) {
                throw new KevoreeAdaptationException("Unable to find a ThreadGroup for instance " + instance.path());
            }
            Thread instanceThread = new Thread(threadGroup, () -> {
                Log.debug("Starting {}", instance.path());
                Thread.currentThread().setContextClassLoader(instanceWrapper.getClassLoader());
                try {
                    instanceWrapper.startInstance();
                } catch (InvocationTargetException e) {
                    error = e;
                }
            }, "kev_instance_" + instance.path());
            instanceThread.start();
            try {
                instanceThread.join(30000);
            } catch (InterruptedException e) {
                throw new KevoreeAdaptationException("Unable to start instance " + instance.path() + " in 30 seconds");
            }

            if (error == null) {
                Log.trace("Started {}", instance.path());
            } else {
                throw new KevoreeAdaptationException("Unable to start instance " + instance.path(), error.getCause());
            }
        } else {
            throw new KevoreeAdaptationException("Unable to find instance " + instance.path());
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new StopInstance(instance, registry).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.START_INSTANCE;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + instance.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "StartInstance    " + instance.path();
    }

    @Override
    public KMFContainer getElement() {
        return instance;
    }
}
