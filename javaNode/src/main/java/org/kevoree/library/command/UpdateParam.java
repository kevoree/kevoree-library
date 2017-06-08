package org.kevoree.library.command;

import org.kevoree.DictionaryAttribute;
import org.kevoree.FragmentDictionary;
import org.kevoree.Instance;
import org.kevoree.Value;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.api.helper.ParamInjector;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;


public class UpdateParam implements AdaptationCommand {

    private Instance instance;
    private Value param;
    private InstanceRegistry registry;
    private String previousValue;

    public UpdateParam(Instance instance, Value param, InstanceRegistry registry) {
        this.instance = instance;
        this.param = param;
        this.registry = registry;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        KInstanceWrapper instanceWrapper = this.registry.get(instance, KInstanceWrapper.class);
        if (instanceWrapper != null) {
            ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(instanceWrapper.getClass().getClassLoader());
            try {
                previousValue = ParamInjector.get(param.getName(), instanceWrapper);
                ParamInjector.inject(param.getName(), param.getValue(), instanceWrapper.getTargetObj());
                debug(param);
                Thread.currentThread().setContextClassLoader(previousCL);
            } catch (Exception e) {
                Thread.currentThread().setContextClassLoader(previousCL);
                throw new KevoreeAdaptationException("Unable to inject value \""+param.getValue()+"\" for parameter \""+param.getName()+"\" in " + instance.path(), e);
            }
        } else {
            throw new KevoreeAdaptationException("Unable to find instance " + instance.path());
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        KInstanceWrapper instanceWrapper = this.registry.get(instance, KInstanceWrapper.class);
        if (instanceWrapper != null) {
            ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(instanceWrapper.getClass().getClassLoader());
            try {
                previousValue = ParamInjector.get(param.getName(), instanceWrapper);
                ParamInjector.inject(param.getName(), previousValue, instanceWrapper.getTargetObj());
                debug(param);
                Thread.currentThread().setContextClassLoader(previousCL);
            } catch (Exception e) {
                Thread.currentThread().setContextClassLoader(previousCL);
                throw new KevoreeAdaptationException("Unable to inject value \""+param.getValue()+"\" for parameter \""+param.getName()+"\" in " + instance.path(), e.getCause());
            }
        } else {
            throw new KevoreeAdaptationException("Unable to find instance " + instance.path());
        }
    }
//
//    private void injectValue(Value kVal, Object obj, ClassLoader previousCL) throws KevoreeAdaptationException {
//        try {
//            ParamInjector.inject(kVal.getName(), kVal.getValue(), obj);
//            debug(param);
//            Thread.currentThread().setContextClassLoader(previousCL);
//        } catch (Exception e) {
//            Thread.currentThread().setContextClassLoader(previousCL);
//            throw new KevoreeAdaptationException("Unable to inject value \""+kVal.getValue()+"\" for parameter \""+kVal.getName()+"\" in " + instance.path(), e.getCause());
//        }
//    }
//
//    private void injectValue(Value kVal, KInstanceWrapper instanceWrapper, ClassLoader previousCL)
//            throws KevoreeAdaptationException {
//        injectValue(kVal, instanceWrapper.getTargetObj(), previousCL);
//    }

    private void debug(Value param) {
        Instance instance = (Instance) param.eContainer().eContainer();
        DictionaryAttribute attr = instance.getTypeDefinition().getDictionaryType().findAttributesByID(param.getName());
        if (attr.getFragmentDependant()) {
            Log.debug("Update param {}.{}/{} = '{}'", instance.getName(), param.getName(),
                    ((FragmentDictionary) param.eContainer()).getName(), param.getValue());
        } else {
            Log.debug("Update param {}.{} = '{}'", instance.getName(), param.getName(), param.getValue());
        }
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.UPDATE_PARAM;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + param.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "UpdateParam      " + param.path() + " = '" + param.getValue() + "'";
    }

    @Override
    public KMFContainer getElement() {
        return param;
    }
}
