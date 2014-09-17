package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService
import org.kevoree.library.cloud.docker.DockerNode
import org.kevoree.library.java.wrapper.WrapperFactory
import org.kevoree.library.java.wrapper.KInstanceWrapper

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
class DockerWrapperFactory(nodeName: String, val dockerNode: DockerNode) : WrapperFactory(nodeName) {

    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService, modelService: ModelService): KInstanceWrapper {
        when (modelElement) {
            is ContainerNode -> {
                return DockerNodeWrapper(modelElement, newBeanInstance, tg, bs, dockerNode)
            } else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs, modelService)
            }
        }
    }
}