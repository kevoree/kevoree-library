package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.api.RemoteChannelFragment

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/12/13
 * Time: 15:02
 *
 * @author Erwan Daubert
 * @version 1.0
 */
 public class RemoteChannelFragmentWrapper(private val _nodeName : String, private val _fragmentDictionaryPath : String) : RemoteChannelFragment {

     override fun getRemoteNodeName(): String? {
         return _nodeName
     }
     override fun getFragmentDictionaryPath(): String? {
         return _fragmentDictionaryPath
     }

 }