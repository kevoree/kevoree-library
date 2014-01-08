package org.kevoree.library.defaultNodeTypes.wrapper

import java.util.HashMap
import org.kevoree.api.ChannelContext
import org.kevoree.api.Port
import org.kevoree.api.RemoteChannelFragment
import org.kevoree.Channel
import org.kevoree.ContainerNode

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 08:46
 */

public class ChannelWrapperContext(val channel : Channel, val localNodeName : String) : ChannelContext {
    val portsBinded: MutableMap<String, Port> = HashMap<String, Port>()

    private var remoteFragmentResolved: Boolean = false
    private val remoteNodeFragments: MutableMap<String, RemoteChannelFragment> = HashMap<String, RemoteChannelFragment>()

    override fun getLocalPorts(): MutableList<Port>? {
        return portsBinded.values().toList() as MutableList<Port>
    }
    
    override fun getRemoteFragments(): MutableList<RemoteChannelFragment>? {
        if (!remoteFragmentResolved) {
            remoteFragmentResolved = true
            manageRemoteConnection();
        }
        return remoteNodeFragments.values().toList() as MutableList<RemoteChannelFragment>
    }


    private fun manageRemoteConnection() {
        channel.bindings.forEach { binding ->
        val nodeName = (binding.port!!.eContainer()!!.eContainer()!! as ContainerNode).name;
        if (!nodeName.equals(localNodeName)) {
            if (!remoteNodeFragments.containsKey(nodeName)) {
                remoteNodeFragments.put(nodeName!!, RemoteChannelFragmentWrapper(nodeName, channel.findFragmentDictionaryByID(nodeName)!!.path()!!))
            }
        }
    }
}
}
