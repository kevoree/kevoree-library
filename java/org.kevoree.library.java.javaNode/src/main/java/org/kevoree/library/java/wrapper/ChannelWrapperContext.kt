package org.kevoree.library.java.wrapper

import java.util.HashMap
import org.kevoree.api.ChannelContext
import org.kevoree.api.Port
import org.kevoree.api.ModelService
import org.kevoree.Channel
import java.util.ArrayList
import org.kevoree.ContainerNode

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 08:46
 */

public class ChannelWrapperContext(val channelPath: String, val localNodePath: String, val modelService: ModelService) : ChannelContext {

    override fun getRemotePortPaths(): MutableList<String>? {
        val result = ArrayList<String>()
        val channel = modelService.getCurrentModel()?.getModel()?.findByPath(channelPath) as? Channel
        if (channel != null) {
            for (binding in channel.bindings) {
                if ( (binding.port?.eContainer()?.eContainer() as? ContainerNode)?.name != localNodePath) {
                    if (binding.port != null) {
                        result.add(binding.port!!.path())
                    }
                }
            }
        }
        return result
    }

    val portsBinded: MutableMap<String, Port> = HashMap<String, Port>()

    override fun getLocalPorts(): MutableList<Port>? {
        return portsBinded.values().toList() as MutableList<Port>
    }

}
