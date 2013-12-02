package org.kevoree.library.defaultNodeTypes.wrapper;

import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.library.defaultNodeTypes.reflect.FieldAnnotationResolver
import java.lang.reflect.InvocationTargetException
import org.kevoree.api.BootstrapService
import org.kevoree.api.ChannelContext
import org.kevoree.api.ChannelDispatch
import org.kevoree.ContainerRoot
import org.kevoree.log.Log

public class ChannelWrapper(override val targetObj: Any, val _nodeName: String, val _name: String, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    val context: ChannelWrapperContext

    {
        context = ChannelWrapperContext()
        bs.injectService(javaClass<ChannelContext>(), context, targetObj)
    }

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    private val fieldResolver = FieldAnnotationResolver(targetObj.javaClass);

    fun call(payload: Any?, callback: org.kevoree.api.Callback?) {
        (targetObj as ChannelDispatch).dispatch(payload, callback)
    }

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!isStarted) {
            try {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Start>())
                met?.invoke(targetObj)
                isStarted = true
                return true
            }catch(e: InvocationTargetException){
                Log.error("Kevoree Channel Instance Start Error !", e.getCause())
                return false
            }catch(e: Exception) {
                Log.error("Kevoree Channel Instance Start Error !", e)
                return false
            }
        } else {
            Log.error("Try to start the channel {} while it is already start", _name)
            return false
        }
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted) {
            try {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Stop>())
                met?.invoke(targetObj)
                isStarted = false
                return true
            }catch(e: InvocationTargetException){
                Log.error("Kevoree Channel Instance Stop Error !", e.getCause())
                return false
            } catch(e: Exception) {
                Log.error("Kevoree Channel Instance Stop Error !", e)
                return false
            }
        } else {
            return false
        }
    }


}





