package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.library.defaultNodeTypes.wrapper.ChannelWrapper
import org.kevoree.api.Port
import org.kevoree.api.Callback
import org.kevoree.log.Log

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

class RequiredPortImpl(val portPath: String) : Port {
    override fun call(payload: Any?, callback: Callback<out Any?>?) {
        if (delegate != null) {
            delegate!!.call(callback,payload)
            //todo send and put the callback inside
        } else {
            callback?.onError(Exception("Message lost, because port is not bind"))
            Log.warn("Message lost, because no binding found : {}", payload.toString())
        }
    }
    override fun send(payload: Any?) {
        call(payload,null)
    }

    override fun getPath(): String? {
        return portPath
    }

    var delegate: ChannelWrapper? = null

}
