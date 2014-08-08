package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
class DockerWrapperFactory(nodeName: String, modelService: ModelService) : WrapperFactory(nodeName) {

    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService, modelService: ModelService): KInstanceWrapper {
        when (modelElement) {
            is ContainerNode -> {
                if (modelElement.typeDefinition!!.name.equals("DockerNode")) {
                    return DockerNodeWrapper(modelElement, newBeanInstance, tg, bs, modelService)
                } else {
                    throw Exception("Unable to process subNode of type "+modelElement.typeDefinition!!.name)
                }
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs, modelService)
            }
        }
    }
}