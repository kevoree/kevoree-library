package org.kevoree.library.java.command;

import org.kevoree.*;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;

import java.lang.reflect.Field;


public class UpdateDictionary implements PrimitiveCommand {

    private Instance c;
    private Value dicValue;
    private String nodeName;
    private ModelRegistry registry;
    private org.kevoree.api.BootstrapService bs;
    private ModelService modelService;
    private boolean forceInject;

    public UpdateDictionary(Instance c, Value dicValue, boolean forceInject, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.c = c;
        this.dicValue = dicValue;
        this.forceInject = forceInject;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public boolean execute() {
        if (!forceInject) {
            ContainerRoot previousModel = modelService.getCurrentModel().getModel();
            KMFContainer previousValue = previousModel.findByPath(dicValue.path());
            if (previousValue == null) {
                Instance instance = (Instance) dicValue.eContainer().eContainer();
                if (instance != null) {
                    KMFContainer previousInstance = previousModel.findByPath(c.path());
                    if (previousInstance != null) {
                        DictionaryType dt = instance.getTypeDefinition().getDictionaryType();
                        DictionaryAttribute dicAtt = dt.findAttributesByID(dicValue.getName());
                        if (dicAtt != null && dicAtt.getDefaultValue() != null
                                && dicAtt.getDefaultValue().equals(dicValue.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }

        Object ref = registry.lookup(c);
        if (ref != null && ref instanceof KInstanceWrapper) {
            doInject(((KInstanceWrapper) ref).getTargetObj());
            return true;
        } else {
            Log.error("Unable to update dictionary of unknown instance: " + c.getName());
            return false;
        }
    }

    public void undo() {
        try {
            // try to find old value
            String valueToInject = null;
            KMFContainer previousValue = modelService.getCurrentModel().getModel().findByPath(dicValue.path());
            if (previousValue != null && previousValue instanceof Value) {
                valueToInject = ((Value) previousValue).getValue();
            } else {
                Instance instance = (Instance) dicValue.eContainer().eContainer();
                DictionaryAttribute dicAtt = instance.getTypeDefinition().getDictionaryType().findAttributesByID(dicValue.getName());
                if (dicAtt.getDefaultValue() != null && !dicAtt.getDefaultValue().equals("")) {
                    valueToInject = dicAtt.getDefaultValue();
                }
            }
            if (valueToInject != null) {
                Value fakeDicoValue = new DefaultKevoreeFactory().createValue();
                fakeDicoValue.setValue(valueToInject);
                fakeDicoValue.setName(dicValue.getName());
                Object reffoundO = registry.lookup(c);
                if (reffoundO != null) {
                    if (reffoundO instanceof KInstanceWrapper) {
                        KInstanceWrapper reffound = (KInstanceWrapper) reffoundO;
                        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(reffound.getTargetObj().getClass().getClassLoader());
                        bs.injectDictionaryValue(fakeDicoValue, reffound.getTargetObj());
                        Thread.currentThread().setContextClassLoader(previousCL);
                    } else {
                        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(reffoundO.getClass().getClassLoader());
                        bs.injectDictionaryValue(fakeDicoValue, reffoundO);
                        Thread.currentThread().setContextClassLoader(previousCL);
                    }
                }
            }
        } catch (Throwable e) {
            Log.debug("Error during rollback ", e);
        }
    }

    private void doInject(Object target) {
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(target.getClass().getClassLoader());
        bs.injectDictionaryValue(dicValue, target);
        debug(dicValue);
        Thread.currentThread().setContextClassLoader(previousCL);
    }

    private void debug(Value value) {
        Instance instance = (Instance) value.eContainer().eContainer();
        DictionaryAttribute attr = instance.getTypeDefinition().getDictionaryType().findAttributesByID(value.getName());
        if (attr.getFragmentDependant()) {
            Log.debug("Update param for {}.{}/{} = '{}'", instance.getName(), value.getName(),
                    ((FragmentDictionary) value.eContainer()).getName(), value.getValue());
        } else {
            Log.debug("Update param for {}.{} = '{}'", instance.getName(), value.getName(), value.getValue());
        }
    }

    private Field lookup(String name, Class clazz) {
        Field f = null;
        for (Field loopf : clazz.getDeclaredFields()) {
            if (name.equals(loopf.getName())) {
                f = loopf;
            }
        }
        if (f != null) {
            return f;
        } else {
            for (Class loopClazz : clazz.getInterfaces()) {
                f = lookup(name, loopClazz);
                if (f != null) {
                    return f;
                }
            }
            if (clazz.getSuperclass() != null) {
                f = lookup(name, clazz.getSuperclass());
                if (f != null) {
                    return f;
                }
            }
        }
        return f;
    }

    public String toString() {
        return "UpdateDictionary " + c.getName();
    }

}
