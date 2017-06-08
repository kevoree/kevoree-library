package org.kevoree.library.command;

import org.kevoree.Instance;
import org.kevoree.KevoreeCoreException;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.service.ModelService;
import org.kevoree.service.RuntimeService;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.library.wrapper.WrapperFactory;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 17:53
 */
public class AddInstance implements AdaptationCommand {

    private WrapperFactory wrapperFactory;
    private Instance instance;
    private InstanceRegistry registry;
    private RuntimeService runtimeService;
    private ModelService modelService;

    public AddInstance(WrapperFactory wrapperFactory, Instance instance, InstanceRegistry registry,
                       RuntimeService runtimeService, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.instance = instance;
        this.registry = registry;
        this.runtimeService = runtimeService;
        this.modelService = modelService;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        try {
            FlexyClassLoader fcl = runtimeService.installTypeDefinition(instance);
            Object instanceObject = runtimeService.createInstance(instance, fcl);
            KInstanceWrapper instanceWrapper = wrapperFactory.wrap(instance, instanceObject, runtimeService, modelService);
            instanceWrapper.setClassLoader(fcl);
//            runtimeService.injectDictionary(instance, instanceObject, true);

            registry.put(instance, instanceWrapper);
            Thread.currentThread().setContextClassLoader(null);
            Log.debug("Instance created {}", instance.path());
        } catch (KevoreeCoreException e) {
            throw new KevoreeAdaptationException("Unable to create instance " + instance.path(), e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new RemoveInstance(wrapperFactory, instance, registry, runtimeService, modelService).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.ADD_INSTANCE;
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
        return "AddInstance      " + instance.path();
    }

    @Override
    public KMFContainer getElement() {
        return instance;
    }
}
