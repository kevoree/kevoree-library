package org.kevoree.library.defaultNodeTypes.wrapper.port

import org.kevoree.api.Port
import org.kevoree.api.Callback
import java.lang.reflect.Method
import org.kevoree.annotation.ProvidedPort
import org.kevoree.log.Log

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:52
 */

class ProvidedPortImpl(targetObj: Any, name: String, val portPath : String) : Port {
    override fun getPath(): String? {
        return portPath;
    }

    var targetMethod: Method? = null

    {
        for(method in targetObj.javaClass.getDeclaredMethods()){
            if(method.getName() == name){
                if(method.getAnnotation(javaClass<ProvidedPort>()) != null){
                    targetMethod = method;
                    // method.getParameterTypes()
                    //todo
                }
            }
        }
    }

    override fun call(payload: Any?, callback: Callback?) {
        try {
            callback?.run(targetMethod?.invoke(payload))
        } catch (e: Throwable){
            Log.error("This is really bad, exception during port call...", e)
        }

    }

}