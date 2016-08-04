package org.kevoree.library.java.command;

import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.api.helper.KModelHelper;
import org.kevoree.log.Log;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

public class RemoveDeployUnit implements PrimitiveCommand {

	private Instance instance;
    private DeployUnit du;
    private BootstrapService bootstrap;

    public RemoveDeployUnit(Instance instance, DeployUnit du, BootstrapService bootstrap) {
    	this.instance = instance;
        this.du = du;
        this.bootstrap = bootstrap;
    }

    public void undo() {
        new AddDeployUnit(instance, du, bootstrap).execute();
    }

    public boolean execute() {
        try {
            bootstrap.removeDeployUnit(du);
            //TODO cleanup links
            return true;

        }catch (Exception e) {
        	Log.error("Unable to RemoveDeployUnit for {}", e, instance.path());
            return false;
        }
    }

   public String toString() {
        return "RemoveDeployUnit " + KModelHelper.fqnGroup(du) + "/" + du.getName() + "/" + du.getVersion() + "/" + du.getHashcode();
    }
}