package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.api.Port
import org.kevoree.api.Callback
import java.lang.reflect.Method
import org.kevoree.log.Log
import org.kevoree.annotation.Input
import org.kevoree.library.defaultNodeTypes.wrapper.ComponentWrapper

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
                    result = targetMethod?.invoke(targetObj)
                } else {
                    if (paramSize == 1) {
                        result = targetMethod?.invoke(targetObj, payload)
                    } else {
                        if (payload is Array<*>) {
                            if (payload.size == paramSize) {
                                result = targetMethod?.invoke(targetObj, payload)
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

    {

        for (method in targetObj.javaClass.getDeclaredMethods()) {
            if (method.getName() == name) {
                if (method.getAnnotation(javaClass<Input>()) != null) {
                    targetMethod = method;
                    paramSize = method.getParameterTypes()!!.size
                }
            }
        }
        if (targetMethod == null) {
            Log.error("Warning Provided port is not binded ... for name " + name)
        }
    }

}