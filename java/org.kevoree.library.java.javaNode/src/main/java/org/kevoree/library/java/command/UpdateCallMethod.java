package org.kevoree.library.java.command;

import org.kevoree.Instance;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class UpdateCallMethod implements PrimitiveCommand {

    private Instance c;
    private ModelRegistry registry;

    public UpdateCallMethod(Instance c, ModelRegistry registry) {
        this.c = c;
        this.registry = registry;
    }

    public boolean execute() {
        Object ref = registry.lookup(c);
        if (ref != null && ref instanceof KInstanceWrapper) {
            KInstanceWrapper instanceWrapper = (KInstanceWrapper) ref;
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(instanceWrapper.getTargetObj().getClass().getClassLoader());
                instanceWrapper.updateInstance();
                Thread.currentThread().setContextClassLoader(classLoader);
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Unable to invoke update method on " + c.path(), e.getCause());
                return false;
            }
        } else {
            Log.error("Unable to find object instance \""+c.path()+"\" (or incompatible with KInstanceWrapper)");
            return false;
        }
    }

    public void undo() {
        //Log.error("Rollback update dictionary not supported yet !!!")
    }

    public String toString() {
        return "UpdateCallMethod " + c.getName();
    }

}
