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
    override fun send(vararg payload: Any?) {
        call(null, payload)
    }

    override fun call(callback: Callback<out Any?>?, vararg payload: Any?) {
        try {
            if (componentWrapper.isStarted) {
                var result: Any? = null
                if (parameter) {
                    result = targetMethod?.invoke(targetObj, payload)
                } else {
                    result = targetMethod?.invoke(targetObj)
                }
                if(callback!= null){
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
    var parameter = false

    {
        for (method in targetObj.javaClass.getDeclaredMethods()) {
            if (method.getName() == name) {
                if (method.getAnnotation(javaClass<Input>()) != null) {
                    targetMethod = method;
                    if (method.getParameterTypes()!!.size == 1) {
                        parameter = true
                    }
                }
            }
        }
        if (targetMethod == null) {
            Log.error("Warning Provided port is not binded ... for name " + name)
        }
    }

}