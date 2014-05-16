package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService
import org.kevoree.library.cloud.lightlxc.wrapper.NetworkGenerator
import org.kevoree.library.cloud.lightlxc.wrapper.IpModelUpdater

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:59
 */

class LightLXCWrapperFactory(nodeName: String,   val hostitfname: String,
                             val hostitfip: String, val containeripbaseaddress: String,
                             val bridgeName : String,val sshdStart : Boolean, val ipStep:Int,
                             val ipStart:Int,val netmask:String,val updater: IpModelUpdater) : WrapperFactory(nodeName) {



    public var wrap :LightLXCNodeWrapper?=null


    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService,modelService: ModelService): KInstanceWrapper {
        when(modelElement) {
            is ContainerNode -> {
                //println("pass par là " + modelElement.name!!)
                val ng = NetworkGenerator(this.hostitfip, this.containeripbaseaddress, ipStep, ipStart)
                val gw = ng.generateGW( modelElement.name)
                val ip = ng.generateIP( modelElement.name)
                val mac = ng.generateMAC( modelElement.name)
                updater.addIpName(ip,  modelElement.name)

                wrap =  LightLXCNodeWrapper(modelElement, newBeanInstance, tg, bs, hostitfname, hostitfip, containeripbaseaddress, bridgeName, sshdStart, ip!!, gw!!, netmask, mac!!)


                return wrap!!;
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs,modelService)
            }
        }
    }
}
