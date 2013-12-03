package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.modeling.api.KMFContainer
import org.kevoree.ComponentInstance
import org.kevoree.Group
import org.kevoree.Channel
import org.kevoree.ContainerNode
import org.kevoree.api.BootstrapService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:33
 */

open class WrapperFactory(val nodeName: String) {

    open fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService): KInstanceWrapper {
        when(modelElement) {
            is ComponentInstance -> {
                return ComponentWrapper(modelElement, newBeanInstance, nodeName, tg, bs)
            }
            is Group -> {
                return GroupWrapper(modelElement,newBeanInstance, nodeName, tg, bs)
            }
            is Channel -> {
                return ChannelWrapper(modelElement, newBeanInstance, nodeName, tg, bs)
            }
            is ContainerNode -> {
                return NodeWrapper(modelElement, newBeanInstance, nodeName, tg, bs)
            }
            else -> {
                throw Exception("Unknow instance type " + modelElement.metaClassName())
            }
        }
    }

}