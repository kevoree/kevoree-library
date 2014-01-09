package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:59
 */

class DockerWrapperFactory(nodeName: String, modelService : ModelService) : WrapperFactory(nodeName, modelService) {

    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService,modelService: ModelService): KInstanceWrapper {
        when(modelElement) {
            is ContainerNode -> {
                return DockerNodeWrapper(modelElement, newBeanInstance, tg, bs)
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs,modelService)
            }
        }
    }


}