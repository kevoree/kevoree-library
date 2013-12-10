package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerNode

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:59
 */

class LightLXCWrapperFactory(nodeName: String) : WrapperFactory(nodeName) {

    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService): KInstanceWrapper {
        when(modelElement) {
            is ContainerNode -> {
                return LightLXCNodeWrapper(modelElement, newBeanInstance, tg, bs)
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs)
            }
        }
    }


}