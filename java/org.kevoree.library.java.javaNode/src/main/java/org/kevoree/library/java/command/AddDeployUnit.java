package org.kevoree.library.java.command;

import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.api.helper.KModelHelper;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.log.Log;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

public class AddDeployUnit implements PrimitiveCommand {

	private Instance instance;
    private DeployUnit du;
    private BootstrapService bs;

    public AddDeployUnit(Instance instance, DeployUnit du, BootstrapService bs) {
    	this.instance = instance;
        this.du = du;
        this.bs = bs;
    }

    public void undo() {
        new RemoveDeployUnit(instance, du, bs).execute();
    }

    public boolean execute() {
        try {
            FlexyClassLoader fcl = bs.installDeployUnit(du);
            return fcl != null;
        } catch (Exception e) {
            Log.error("Unable to AddDeployUnit for {}", e, instance.path());
            return false;
        }
    }

    public String toString() {
        return "AddDeployUnit " + KModelHelper.fqnGroup(du) + "/" + du.getName() + "/" + du.getVersion() + "/" + du.getHashcode();
    }


}