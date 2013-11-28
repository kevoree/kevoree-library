package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.api.ChannelContext
import org.kevoree.api.Port
import java.util.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 08:46
 */

public class ChannelWrapperContext : ChannelContext {

    val portsBinded: MutableMap<String, Port> = HashMap<String, Port>()

    override fun getLocalPorts(): MutableList<Port>? {
        return portsBinded.values().toList() as MutableList<Port>
    }

}
