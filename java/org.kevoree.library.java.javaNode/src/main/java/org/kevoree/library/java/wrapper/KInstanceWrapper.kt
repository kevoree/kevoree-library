package org.kevoree.library.java.wrapper

import org.kevoree.library.java.reflect.MethodAnnotationResolver
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 18/11/2013
 * Time: 16:53
 */

public trait KInstanceWrapper {

    var tg: ThreadGroup

    var isStarted: Boolean

    fun kInstanceStart(tmodel: ContainerRoot): Boolean

    fun kInstanceStop(tmodel: ContainerRoot): Boolean

    val resolver: MethodAnnotationResolver

    val targetObj: Any

    val bs: BootstrapService

    var kcl : ClassLoader?

    fun create() {

    }

    fun destroy() {
    }

}
