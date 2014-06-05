package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.Instance
import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.api.BootstrapService
import org.kevoree.api.ModelService
import org.kevoree.api.PrimitiveCommand

/**
 * Created by duke on 6/5/14.
 */

class UpgradeInstance(val wrapperFactory: WrapperFactory, val c: Instance, val nodeName: String, val registry: ModelRegistry, val bs: BootstrapService, val modelService: ModelService) : PrimitiveCommand {

    val remove_cmd = RemoveInstance(wrapperFactory, c, nodeName, registry, bs, modelService)
    val add_cmd = AddInstance(wrapperFactory, c, nodeName, registry, bs, modelService)

    override fun execute(): Boolean {
        if (remove_cmd.execute()) {
            return add_cmd.execute()
        } else {
            return false
        }
    }

    override fun undo() {
        add_cmd.undo()
        remove_cmd.undo()
    }


}