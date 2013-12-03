package org.kevoree.library.defaultNodeTypes.wrapper

import java.lang.reflect.InvocationTargetException
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.Instance
import org.kevoree.log.Log

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

    fun kUpdateDictionary(tmodel: ContainerRoot, instance: Instance): Boolean {
        try {
            bs.injectDictionary(instance, targetObj)
            if (isStarted) {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Update>())
                met?.invoke(targetObj)
            }
            return true
        } catch(e: InvocationTargetException){
            Log.error("Kevoree Component Instance Update Error !", e.getCause())
            return false
        } catch(e: Exception) {
            Log.error("Kevoree Component Instance Update Error !", e)
            return false
        }
    }

    fun create(){

    }

    fun destroy(){

    }

}
