package org.kevoree.library.compare;

import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.MBinding;
import org.kevoree.Value;
import org.kevoree.api.ModelService;
import org.kevoree.api.RuntimeService;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.command.*;
import org.kevoree.library.wrapper.WrapperFactory;

/**
 *
 * Created by leiko on 3/2/17.
 */
public class CommandFactory {

    private String nodeName;
    private RuntimeService runtimeService;
    private ModelService modelService;
    private WrapperFactory wrapperFactory;
    private InstanceRegistry instanceRegistry;

    public CommandFactory(String nodeName, RuntimeService runtime, ModelService model, InstanceRegistry registry, WrapperFactory factory) {
        this.nodeName = nodeName;
        this.runtimeService = runtime;
        this.modelService = model;
        this.instanceRegistry = registry;
        this.wrapperFactory = factory;
    }

    public AddDeployUnit createAddDeployUnit(DeployUnit deployUnit) {
        return new AddDeployUnit(deployUnit, runtimeService);
    }

    public AddInstance createAddInstance(Instance instance) {
        return new AddInstance(wrapperFactory, instance, instanceRegistry, runtimeService, modelService);
    }

    public AddBinding createAddBinding(MBinding binding) {
        return new AddBinding(binding, nodeName, instanceRegistry);
    }

    public RemoveBinding createRemoveBinding(MBinding binding) {
        return new RemoveBinding(binding, nodeName, instanceRegistry);
    }

    public RemoveDeployUnit createRemoveDeployUnit(DeployUnit deployUnit) {
        return new RemoveDeployUnit(deployUnit, runtimeService);
    }

    public RemoveInstance createRemoveInstance(Instance instance) {
        return new RemoveInstance(wrapperFactory, instance, instanceRegistry, runtimeService, modelService);
    }

    public StartInstance createStartInstance(Instance instance) {
        return new StartInstance(instance, instanceRegistry);
    }

    public StopInstance createStopInstance(Instance instance) {
        return new StopInstance(instance, instanceRegistry);
    }

    public UpdateParam createUpdateParam(Instance instance, Value value) {
        return new UpdateParam(instance, value, instanceRegistry);
    }

    public UpdateInstance createUpdateInstance(Instance instance) {
        return new UpdateInstance(instance, instanceRegistry);
    }
}
