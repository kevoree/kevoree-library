package org.kevoree.library.java.wrapper.port

import org.kevoree.api.Port
import org.kevoree.api.Callback
import java.lang.reflect.Method
import org.kevoree.log.Log
import org.kevoree.library.java.wrapper.ComponentWrapper
import java.lang.invoke.MethodHandle
import java.util.ArrayList

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:52
 */

class ProvidedPortImpl(val targetObj: Any, name: String, val portPath: String, val componentWrapper: ComponentWrapper) : Port {

    override fun getConnectedBindingsSize(): Int {
        throw UnsupportedOperationException()
    }

    override fun send(payload: Any?) {
        call(payload, null)
    }

    private val pending = ArrayList<StoredCall>()

    data class StoredCall(val payload: Any?, val callback: Callback<out Any?>?)

    override fun call(payload: Any?, callback: Callback<out Any?>?) {
        try {
            if (componentWrapper.isStarted) {
                var result: Any? = null
                if (paramSize == 0) {
                    if (methodHandler != null) {
                        result = methodHandler?.invokeExact(targetObj)
                    } else {
                        result = targetMethod?.invoke(targetObj)
                    }
                } else {
                    if (paramSize == 1) {
                        if (methodHandler != null) {
                            result = methodHandler?.invokeExact(targetObj, payload)
                        } else {
                            result = targetMethod?.invoke(targetObj, payload)
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
            } else {
                //store call somewhere
                pending.add(StoredCall(payload, callback))
            }
        } catch (e: Throwable) {
            Log.error("This is really bad, exception during port call...", e)
        }
    }

    fun processPending() {
        if (!pending.isEmpty()) {
            val t = Thread(object : Runnable {
                override fun run() {
                    for (c in pending) {
                        call(c.payload, c.callback)
                    }
                    pending.clear()
                }
            })
            t.start()
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