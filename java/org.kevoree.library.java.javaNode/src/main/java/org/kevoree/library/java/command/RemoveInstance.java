package org.kevoree.library.java.command;

import org.kevoree.library.java.ModelRegistry;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.api.ModelService;
import org.kevoree.log.Log;

public class RemoveInstance implements PrimitiveCommand {

    private WrapperFactory wrapperFactory;
    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private BootstrapService bs;
    private ModelService modelService;

    public RemoveInstance(WrapperFactory wrapperFactory, Instance c, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public void undo() {
        new AddInstance(wrapperFactory, c, nodeName, registry, bs, modelService).execute();
    }

    public boolean execute() {
        try {
            Object ref = registry.lookup(c);
            if (ref instanceof KInstanceWrapper) {
                ((KInstanceWrapper) ref).destroy();
            }
            registry.drop(c);
            Log.info("Remove instance {}", c.path());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

   public String toString() {
        return "RemoveInstance " + c.getName();
    }

}
