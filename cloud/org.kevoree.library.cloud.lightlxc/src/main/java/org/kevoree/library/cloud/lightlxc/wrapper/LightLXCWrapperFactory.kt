package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerNode
import org.kevoree.api.ModelService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:59
 */

class LightLXCWrapperFactory(nodeName: String, val routeditfname : String ,val hostitfname : String , val hostitfip : String ,val containeripbaseaddress : String,val createBrdge : Boolean,
                             val bridgeName : String, val ipStep : Int , val ipStart : Int, val networkMask:String ,val sshdStart : Boolean) : WrapperFactory(nodeName) {

    public var wrap :LightLXCNodeWrapper?=null


    override fun wrap(modelElement: KMFContainer, newBeanInstance: Any, tg: ThreadGroup, bs: BootstrapService,modelService: ModelService): KInstanceWrapper {
        when(modelElement) {
            is ContainerNode -> {
                wrap =  LightLXCNodeWrapper(modelElement, newBeanInstance, tg, bs,routeditfname, hostitfname,hostitfip,containeripbaseaddress, createBrdge, bridgeName,ipStep,ipStart,networkMask,sshdStart)
                return wrap!!;
            }
            else -> {
                return super.wrap(modelElement, newBeanInstance, tg, bs,modelService)
            }
        }
    }


}