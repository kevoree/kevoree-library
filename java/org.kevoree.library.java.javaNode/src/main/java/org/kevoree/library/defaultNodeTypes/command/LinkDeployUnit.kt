package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.api.PrimitiveCommand
import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.DeployUnit
import org.kevoree.log.Log
import java.util.ArrayList
import org.kevoree.kcl.api.FlexyClassLoader

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 14:14
 */

public class LinkDeployUnit(val du: DeployUnit, val bs: org.kevoree.api.BootstrapService, val registry: ModelRegistry) : PrimitiveCommand {

    var kcls = ArrayList<FlexyClassLoader>()

    override fun execute(): Boolean {
        var installedKCL = bs.get(du)
        if (installedKCL == null) {
            Log.error("DeployUnit not installed !! {}", du.path())
            return false
        }
        for (ldu in du.requiredLibs) {
            var subresolved = bs.get(ldu)
            if (subresolved == null) {
                Log.error("DeployUnit not installed !! {}", ldu.path())
                return false
            } else {
                kcls.add(subresolved!!)
            }
        }
        for (subKCL in kcls) {
            installedKCL!!.attachChild(subKCL)
        }
        return true
    }

    override fun undo() {
        var installedKCL = bs.get(du)
        if (installedKCL != null) {
            for (subKCL in kcls) {
                installedKCL!!.detachChild(subKCL)
            }
        }
    }

}