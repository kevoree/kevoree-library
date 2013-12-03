package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class DockerNodeWrapper(val modelElement: ContainerNode,override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {

        System.out.println("Start lxc node ....");

        //TODO non blocking start Docker Child node here
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {

        System.out.println("Stop lxc node ....");


        //TODO non blocking stop Docker Child node here
        return true
    }


}