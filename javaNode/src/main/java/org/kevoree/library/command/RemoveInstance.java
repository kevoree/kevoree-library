package org.kevoree.library.command;

import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.service.ModelService;
import org.kevoree.service.RuntimeService;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.WrapperFactory;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

public class RemoveInstance implements AdaptationCommand {

    private WrapperFactory wrapperFactory;
    private Instance instance;
    private InstanceRegistry registry;
    private RuntimeService runtimeService;
    private ModelService modelService;

    public RemoveInstance(WrapperFactory wrapperFactory, Instance instance, InstanceRegistry registry, RuntimeService runtimeService, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.instance = instance;
        this.registry = registry;
        this.runtimeService = runtimeService;
        this.modelService = modelService;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        try {
            registry.remove(instance);
            Log.debug("Instance removed {}", instance.path());
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to remove instance " + instance.path(), e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new AddInstance(wrapperFactory, instance, registry, runtimeService, modelService).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.REMOVE_INSTANCE;
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
        return "RemoveInstance   " + instance.path();
    }

    @Override
    public KMFContainer getElement() {
        return instance;
    }
}
