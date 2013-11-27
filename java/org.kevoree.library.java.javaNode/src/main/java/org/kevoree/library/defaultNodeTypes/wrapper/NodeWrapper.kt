package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.ContainerRoot
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.api.BootstrapService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 17/11/2013
 * Time: 20:03
 */

public class NodeWrapper(override val targetObj: Any,nodePath: String, override var tg : ThreadGroup, override val bs : BootstrapService) : KInstanceWrapper {

    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    override var isStarted: Boolean = false

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        System.out.println("Node Should start here")
        System.out.println("Default implementation should be bootstrap")
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        System.out.println("Node Should start here")
        System.out.println("Default implementation should be bootstrap")
        return true
    }

}