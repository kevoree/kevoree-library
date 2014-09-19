package org.kevoree.library.java.command

import org.kevoree.library.java.wrapper.ComponentWrapper
import org.kevoree.library.java.wrapper.ChannelWrapper
import org.kevoree.library.java.ModelRegistry
import org.kevoree.MBinding
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ComponentInstance
import org.kevoree.log.Log

class AddBindingCommand(val c: MBinding, val nodeName: String, val registry: ModelRegistry) : PrimitiveCommand {

    override fun undo() {
        RemoveBindingCommand(c, nodeName, registry).execute()
    }


    override fun execute(): Boolean {
        val kevoreeChannelFound = registry.lookup(c.hub!!)
        val kevoreeComponentFound = registry.lookup(c.port!!.eContainer() as ComponentInstance)
        if (kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound is ComponentWrapper && kevoreeChannelFound is ChannelWrapper) {
            val portName = c.port!!.portTypeRef!!.name!!
            val foundNeedPort = kevoreeComponentFound.requiredPorts.get(portName)
            val foundHostedPort = kevoreeComponentFound.providedPorts.get(portName)
            if (foundNeedPort == null && foundHostedPort == null) {
                Log.info("Port instance {} not found in component", portName)
                return false
            }
            if (foundNeedPort != null) {
                foundNeedPort.delegate.add(kevoreeChannelFound as ChannelWrapper)
                return true
            }
            if (foundHostedPort != null) {
                var component = (c.port!!.eContainer() as ComponentInstance)
                kevoreeChannelFound.context.portsBinded.put("$component/$portName", foundHostedPort)
                return true
            }
            return false
        } else {
            Log.error("Error while apply binding , channelFound=${kevoreeChannelFound}, componentFound=${kevoreeComponentFound}")
            return false
        }
    }

    override fun toString(): String {
        return "AddBindingCommand ${c.hub?.name} <-> ${(c.port?.eContainer() as? ComponentInstance)?.name}"
    }
}
