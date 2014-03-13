package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.library.defaultNodeTypes.wrapper.ChannelWrapper
import org.kevoree.api.Port
import org.kevoree.api.Callback
import org.kevoree.log.Log
import java.util.ArrayList

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

class RequiredPortImpl(val portPath: String) : Port {

    override fun getConnectedBindingsSize(): Int {
        return delegate.size()
    }
    override fun call(payload: Any?, callback: Callback<out Any?>?) {
        if (!delegate.empty) {
            for (wrapper in delegate) {
                wrapper.call(callback, payload)
            }
        } else {
            callback?.onError(Exception("Message lost, because port is not bind"))
            Log.warn("Message lost, because no binding found : {}", payload.toString())
        }
    }
    override fun send(payload: Any?) {
        call(payload, null)
    }

    override fun getPath(): String? {
        return portPath
    }

    var delegate: MutableList<ChannelWrapper> = ArrayList<ChannelWrapper>()

}
