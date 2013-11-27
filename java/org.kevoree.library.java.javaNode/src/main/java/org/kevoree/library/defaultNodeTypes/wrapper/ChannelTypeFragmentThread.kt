package org.kevoree.library.defaultNodeTypes.wrapper;

import java.util.HashMap
import java.util.concurrent.Callable
import org.kevoree.ContainerRoot
import org.kevoree.annotation.LocalBindingUpdated
import org.kevoree.annotation.RemoteBindingUpdated
import org.kevoree.framework.message.FragmentBindMessage
import org.kevoree.framework.message.FragmentUnbindMessage
import org.kevoree.framework.message.Message
import org.kevoree.framework.message.PortUnbindMessage
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.library.defaultNodeTypes.reflect.FieldAnnotationResolver
import org.kevoree.log.Log
import java.lang.reflect.InvocationTargetException
import org.kevoree.framework.AbstractChannelFragment
import org.kevoree.framework.KevoreeChannelFragment
import org.kevoree.framework.ChannelFragment
import org.kevoree.framework.ChannelFragmentSender
import org.kevoree.api.BootstrapService
import org.kevoree.framework.KevoreePort

class ChannelTypeFragmentThread(override val targetObj: AbstractChannelFragment, val _nodeName: String, val _name: String, override var tg: ThreadGroup, override val bs: BootstrapService) : KevoreeChannelFragment, KInstanceWrapper, ChannelFragment {

    public fun initChannel() {
        targetObj.delegate = this
    }

    public override fun dispatch(msg: Message?): Any? {
        if(msg!!.getInOut()){
            return sendWait(msg)
        } else {
            send(msg)
            return null
        }
    }
    public override fun createSender(remoteNodeName: String?, remoteChannelName: String?): ChannelFragmentSender? {
        return targetObj.createSender(remoteNodeName, remoteChannelName)
    }

    var pool: PausablePortThreadPoolExecutor? = null
    val portsBinded: MutableMap<String, KevoreePort> = HashMap<String, KevoreePort>()
    val fragementBinded: MutableMap<String, KevoreeChannelFragment> = HashMap<String, KevoreeChannelFragment>()
    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    private val fieldResolver = FieldAnnotationResolver(targetObj.javaClass);


    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!isStarted) {
            try {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Start>())
                met?.invoke(targetObj)
                isStarted = true
                pool = PausablePortThreadPoolExecutor.newPausableThreadPool(1, tg)
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
                if (pool != null) {
                    pool!!.shutdownNow()
                    pool = null
                }
                //TODO CHECK QUEUE SIZE AND SAVE STATE
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

    override fun send(o: Any?) {
        pool!!.submit(AsyncCall(o))
    }

    override fun sendWait(o: Any?): Any? {
        return pool!!.submit(SyncCall(o)).get()
    }

    inner class AsyncCall(val o: Any?) : Runnable {
        override fun run() {
            when(o) {
                is Message -> {
                    targetObj.dispatch(o)
                }
                else -> {
                    val msg2 = Message()
                    msg2.setInOut(false)
                    msg2.setContent(o!!)
                    targetObj.dispatch(msg2)
                }
            }
        }
    }

    inner class SyncCall(val o: Any?) : Callable<Any> {
        override fun call(): Any? {
            when(o) {
                is Message -> {
                    return dispatch(o)
                }
                else -> {
                    val msg2 = Message()
                    msg2.setInOut(true)
                    msg2.setContent(o!!)
                    return targetObj.dispatch(msg2)
                }
            }
        }
    }

    public override fun forward(delegate: KevoreeChannelFragment?, inmsg: Message?): Any? {
        val msg = inmsg!!.clone()
        // msg.setDestChannelName(delegate!!.getName())
        // msg.setDestNodeName(delegate.getNodeName())
        if (msg.getInOut()) {
            return delegate?.sendWait(msg)
        } else {
            delegate?.send(msg)
            return null
        }
    }

    public override fun forward(delegate: KevoreePort?, inmsg: Message?): Any? {
        /*
        try {
            val msg = inmsg!!.clone()
            msg.setDestChannelName(delegate!!.getName()!!)
            if (msg.getInOut()) {
                return delegate.sendWait(msg.getContent())
            } else {
                delegate.send(msg.getContent())
                return null
            }
        } catch(e: Throwable) {
            Log.error("Error while sending MSG ", e)
            return null
        }
        */
        return null
    }

    override fun processAdminMsg(o: Any): Boolean {
        pool?.pause()
        val res = when(o) {
            is FragmentBindMessage -> {
                val sender = this.createSender((o as FragmentBindMessage).fragmentNodeName, (o as FragmentBindMessage).channelName)
                val proxy = KevoreeChannelFragmentThreadProxy((o as FragmentBindMessage).fragmentNodeName, (o as FragmentBindMessage).channelName)
                proxy.channelSender = sender
                fragementBinded.put(createPortKey(o), proxy)
                //proxy.startC()
                val met = resolver.resolve(javaClass<RemoteBindingUpdated>())
                if (met != null) {
                    met.invoke(targetObj)
                }
                true
            }
            is FragmentUnbindMessage -> {
                val actorPort: KevoreeChannelFragment? = fragementBinded.get(createPortKey(o))
                if (actorPort != null) {
                    //actorPort.stopC()
                    fragementBinded.remove(createPortKey(o))
                    val met = resolver.resolve(javaClass<RemoteBindingUpdated>())
                    if (met != null) {
                        met.invoke(targetObj)
                    }
                    true
                } else {
                    Log.debug("Can't unbind Fragment " + createPortKey(o))
                    false
                }
            }
            is PortUnbindMessage -> {
                portsBinded.remove(createPortKey(o))
                val met = resolver.resolve(javaClass<LocalBindingUpdated>())
                if (met != null) {
                    met.invoke(targetObj)
                }
                true
            }
            else -> {
                false
            }
        }
        pool?.resume()
        return res
    }

    private fun createPortKey(a: Any): String {
        when(a) {
            is PortUnbindMessage -> {
                return (a as PortUnbindMessage).nodeName + "-" + (a as PortUnbindMessage).componentName + "-" + (a as PortUnbindMessage).portName
            }
            is FragmentBindMessage -> {
                return (a as FragmentBindMessage).channelName + "-" + (a as FragmentBindMessage).fragmentNodeName
            }
            is FragmentUnbindMessage -> {
                return (a as FragmentUnbindMessage).channelName + "-" + (a as FragmentUnbindMessage).fragmentNodeName
            }
            else -> {
                return ""
            }
        }
    }


    public override fun getBindedPorts(): MutableList<org.kevoree.framework.KevoreePort>? {
        return portsBinded.values().toList() as MutableList<org.kevoree.framework.KevoreePort>?
    }

    //OVERRIDE BY FACTORY
    public override fun getOtherFragments(): MutableList<org.kevoree.framework.KevoreeChannelFragment>? {
        return fragementBinded.values().toList() as MutableList<org.kevoree.framework.KevoreeChannelFragment>?
    }

}





