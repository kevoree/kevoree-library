package org.kevoree.library.java.wrapper;

import org.kevoree.api.BootstrapService;
import org.kevoree.library.java.reflect.MethodAnnotationResolver;

/**
 * Created by duke on 9/26/14.
 */
public interface KInstanceWrapper {

    private ThreadGroup tg;

    private Boolean isStarted;




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
