package org.kevoree.library.java.command;

import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.Instance;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.api.PrimitiveCommand;

/**
 * Created by duke on 6/5/14.
 */

public class UpgradeInstance implements PrimitiveCommand {

    private WrapperFactory wrapperFactory;
    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private BootstrapService bs;
    private ModelService modelService;

    private RemoveInstance remove_cmd = new RemoveInstance(wrapperFactory, c, nodeName, registry, bs, modelService);
    private AddInstance add_cmd       = new AddInstance(wrapperFactory, c, nodeName, registry, bs, modelService);

    public UpgradeInstance(WrapperFactory wrapperFactory, Instance c, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public boolean execute() {
        return remove_cmd.execute() && add_cmd.execute();
    }

    public void undo() {
        add_cmd.undo();
        remove_cmd.undo();
    }
}