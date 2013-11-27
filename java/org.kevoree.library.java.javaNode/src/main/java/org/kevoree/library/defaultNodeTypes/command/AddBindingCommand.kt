package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.MBinding
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ComponentInstance
import org.kevoree.log.Log
import org.kevoree.library.defaultNodeTypes.wrapper.KevoreeComponent
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelTypeFragmentThread

class AddBindingCommand(val c: MBinding, val nodeName: String, val registry: MutableMap<String, Any>) : PrimitiveCommand {

    override fun undo() {
        RemoveBindingCommand(c, nodeName, registry).execute()
    }
    override fun execute(): Boolean {
        val kevoreeChannelFound = registry.get(c.hub!!.path()!!)
        val kevoreeComponentFound = registry.get((c.port!!.eContainer() as ComponentInstance).path()!!)
        if(kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound is KevoreeComponent && kevoreeChannelFound is KevoreeChannelFragment){
            val portName = c.port!!.portTypeRef!!.name!!
            val foundNeedPort = kevoreeComponentFound.requiredPorts.get(portName)
            val foundHostedPort = kevoreeComponentFound.providedPorts.get(portName)
            if(foundNeedPort == null && foundHostedPort == null){
                Log.info("Port instance {} not found in component", portName)
                return false
            }
            if (foundNeedPort != null) {
                foundNeedPort.delegate = kevoreeChannelFound as ChannelTypeFragmentThread?
                return true
            }
            if(foundHostedPort != null){
                //val compoName = (c.port!!.eContainer() as ComponentInstance).name!!
                //val bindmsg = PortBindMessage(foundHostedPort as KevoreePort, nodeName, compoName, (foundHostedPort as KevoreePort).getName()!!)
                //return kevoreeChannelFound.processAdminMsg(bindmsg)
            }
            return false
        } else {
            Log.error("Error while apply binding , channelFound=${kevoreeChannelFound}, componentFound=${kevoreeComponentFound}")
            return false
        }
    }

    fun toString(): String {
        return "AddBindingCommand ${c.hub?.name} <-> ${(c.port?.eContainer() as? ComponentInstance)?.name}"
    }
}
