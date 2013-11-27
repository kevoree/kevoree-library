package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.api.Port
import org.kevoree.api.Callback
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelTypeFragmentThread
import org.kevoree.log.Log

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

class RequiredPortImpl : Port {

    var delegate: ChannelTypeFragmentThread? = null

    override fun call(payload: Any?, callback: Callback?) {
        if(delegate != null){
            delegate!!.send(payload)
            //todo send and put the callback inside
        } else {
            callback?.run(null)
            Log.warn("Message lost, because no binding found : {}",payload?.toString())
        }
    }

}
