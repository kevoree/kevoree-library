package org.kevoree.library.command;

import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.modeling.api.KMFContainer;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class UpdateInstance extends AbstractAdaptationCommand {

    private Instance instance;
    private InstanceRegistry registry;

    public UpdateInstance(Instance instance, InstanceRegistry registry) {
        this.instance = instance;
        this.registry = registry;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        KInstanceWrapper instanceWrapper = registry.get(instance, KInstanceWrapper.class);
        if (instanceWrapper != null) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(instanceWrapper.getTargetObj().getClass().getClassLoader());
                instanceWrapper.updateInstance();
                Thread.currentThread().setContextClassLoader(classLoader);
            } catch (InvocationTargetException e) {
                throw new KevoreeAdaptationException("Unable to invoke update method on " + instance.path(), e.getCause());
            }
        } else {
            throw new KevoreeAdaptationException("Unable to find instance \""+ instance.path()+"\" (or incompatible with KInstanceWrapper)");
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        // TODO undoing update might mean that we should rollback param values and re-call update
        // Log.warn("Undo-ing @Update method internalDispatch is not supported yet");
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.UPDATE_INSTANCE;
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
        return "UpdateInstance   " + instance.path();
    }

    @Override
    public KMFContainer getElement() {
        return instance;
    }
}
