package org.kevoree.library.java.command;

import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.DeployUnit;
import org.kevoree.log.Log;
import java.util.ArrayList;
import java.util.List;

import org.kevoree.kcl.api.FlexyClassLoader;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 14:14
 */

public class LinkDeployUnit implements PrimitiveCommand {

    private  DeployUnit du;
    private  org.kevoree.api.BootstrapService bs;
    private ModelRegistry registry;


    public LinkDeployUnit(DeployUnit du, BootstrapService bs, ModelRegistry registry) {
        this.du = du;
        this.bs = bs;
        this.registry = registry;
    }

    List<FlexyClassLoader> kcls = new ArrayList<FlexyClassLoader>();

    public boolean execute() {
        FlexyClassLoader installedKCL = bs.get(du);
        if (installedKCL == null) {
            Log.error("DeployUnit not installed !! {}", du.path());
            return false;
        }
        for (DeployUnit ldu : du.getRequiredLibs()) {
            FlexyClassLoader subresolved = bs.get(ldu);
            if (subresolved == null) {
                Log.error("DeployUnit not installed !! {}", ldu.path());
                return false;
            } else {
                kcls.add(subresolved);
            }
        }
        for (FlexyClassLoader subKCL : kcls) {
            installedKCL.attachChild(subKCL);
        }
        return true;
    }

    public void undo() {
        FlexyClassLoader installedKCL = bs.get(du);
        if (installedKCL != null) {
            for (FlexyClassLoader subKCL : kcls) {
                installedKCL.detachChild(subKCL);
            }
        }
    }

}