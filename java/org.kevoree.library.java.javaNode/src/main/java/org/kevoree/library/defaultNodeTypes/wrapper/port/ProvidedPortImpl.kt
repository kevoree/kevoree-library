package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.api.Port
import org.kevoree.api.Callback
import java.lang.reflect.Method
import org.kevoree.log.Log
import org.kevoree.annotation.Input
import org.kevoree.library.defaultNodeTypes.wrapper.ComponentWrapper
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.MethodHandle
import java.beans.MethodDescriptor

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:52
 */

class ProvidedPortImpl(val targetObj: Any, name: String, val portPath: String, val componentWrapper: ComponentWrapper) : Port {
    override fun send(payload: Any?) {
        call(payload, null)
    }

    override fun call(payload: Any?, callback: Callback<out Any?>?) {
        try {
            if (componentWrapper.isStarted) {
                var result: Any? = null
                if (paramSize == 0) {
                    if(methodHandler!=null){
                        result = methodHandler?.invokeExact(targetObj)
                    } else {
                        result = targetMethod?.invoke(targetObj)
                    }
                } else {
                    if (paramSize == 1) {
                        if(methodHandler!=null){
                            result = methodHandler?.invokeExact(targetObj,payload)
                        } else {
                            result = targetMethod?.invoke(targetObj,payload)
                        }
                    } else {
                        if (payload is Array<*>) {
                            if (payload.size == paramSize) {
                                if (targetMethod != null) {
                                    result = CallBackCaller.callMethod(targetMethod, targetObj, payload)
                                }
                            } else {
                                callback?.onError(Exception("Non corresponding parameters, " + paramSize + " expected, found " + payload.size))
                            }
                        } else {
                            callback?.onError(Exception("Non corresponding parameters, " + paramSize + " expected, found : not an array == 1"))
                        }
                    }
                }
                if (callback != null) {
                    CallBackCaller.call(result, callback)
                }
            }
        } catch (e: Throwable){
            Log.error("This is really bad, exception during port call...", e)
        }
    }

    override fun getPath(): String? {
        return portPath;
    }

    var targetMethod: Method? = null
    var paramSize = 0
    var methodHandler: MethodHandle? = null;

    {
        targetMethod = MethodResolver.resolve(name, targetObj.javaClass)
        targetMethod?.setAccessible(true)
        if (targetMethod == null) {
            Log.error("Warning Provided port is not binded ... for name " + name)
        } else {
            paramSize = targetMethod!!.getParameterTypes()!!.size
        }

       // var mt = MethodType.methodType(targetMethod!!.getReturnType()!!,targetMethod!!.getParameterTypes()!!)
      //  methodHandler = MethodHandles.lookup().findVirtual(targetObj.javaClass, name, mt)

    }

}