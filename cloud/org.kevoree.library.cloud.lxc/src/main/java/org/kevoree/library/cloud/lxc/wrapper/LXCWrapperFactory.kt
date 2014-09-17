package org.kevoree.library.cloud.lxc.wrapper

import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService
import org.kevoree.library.java.wrapper.WrapperFactory
import org.kevoree.library.java.wrapper.KInstanceWrapper

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:59
 */

class LXCWrapperFactory(nodeName: String, val lxc : LxcManager) : WrapperFactory(nodeName) {

    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService,modelService: ModelService): KInstanceWrapper {
        when(modelElement) {
            is ContainerNode -> {
                return LXCNodeWrapper(modelElement, lxc, newBeanInstance, tg, bs)
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs,modelService)
            }
        }
    }


}