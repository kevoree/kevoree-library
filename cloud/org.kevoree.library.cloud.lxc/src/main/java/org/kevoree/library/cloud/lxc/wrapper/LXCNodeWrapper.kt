package org.kevoree.library.cloud.lxc.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import org.kevoree.log.Log

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class LXCNodeWrapper(val modelElement: ContainerNode, val lxc: LxcManager, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        return  lxc.start_container(modelElement)
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        return   lxc.stop_container(modelElement);
    }

    override fun create() {
        lxc.create_container(modelElement);
    }

    override fun destroy()
    {
        lxc.destroy_container(modelElement);
    }

}