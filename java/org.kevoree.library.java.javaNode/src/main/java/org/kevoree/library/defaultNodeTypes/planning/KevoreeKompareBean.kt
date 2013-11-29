package org.kevoree.library.defaultNodeTypes.planning

import org.kevoree.*
import org.kevoreeadaptation.*

open class KevoreeKompareBean(registry: Map<String, Any>) : Kompare4(registry), KevoreeScheduler {
    override var adaptationModelFactory: KevoreeAdaptationFactory = org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory()

    fun plan(actualModel: ContainerRoot, targetModel: ContainerRoot, nodeName: String): AdaptationModel {
        var adaptationModel = compareModels(actualModel, targetModel, nodeName)
        val afterPlan = schedule(adaptationModel, nodeName)
        return afterPlan
    }

    private fun transformPrimitives(adaptationModel: AdaptationModel, actualModel: ContainerRoot): AdaptationModel {
        //TRANSFORME UPDATE
        for(adaptation in adaptationModel.adaptations){
            when(adaptation.primitiveType) {
                JavaPrimitive.UpdateBinding.name() -> {
                    val rcmd = adaptationModelFactory.createAdaptationPrimitive()
                    rcmd.primitiveType = JavaPrimitive.RemoveBinding.name()
                    rcmd.ref = adaptation.ref!!
                    adaptationModel.removeAdaptations(adaptation)
                    adaptationModel.addAdaptations(rcmd)

                    val acmd = adaptationModelFactory.createAdaptationPrimitive()
                    acmd.primitiveType = JavaPrimitive.AddBinding.name()
                    acmd.ref = adaptation.ref!!
                    adaptationModel.addAdaptations(acmd)
                }
                JavaPrimitive.UpdateInstance.name() -> {
                    val stopcmd = adaptationModelFactory.createAdaptationPrimitive()
                    stopcmd.primitiveType = JavaPrimitive.StopInstance.name()
                    stopcmd.ref = (adaptation.ref as Array<Any>).get(0)
                    adaptationModel.removeAdaptations(adaptation)
                    adaptationModel.addAdaptations(stopcmd)

                    val rcmd = adaptationModelFactory.createAdaptationPrimitive()
                    rcmd.primitiveType = JavaPrimitive.RemoveInstance.name()
                    rcmd.ref = (adaptation.ref as Array<Any>).get(0)
                    adaptationModel.removeAdaptations(adaptation)
                    adaptationModel.addAdaptations(rcmd)

                    val acmd = adaptationModelFactory.createAdaptationPrimitive()
                    acmd.primitiveType = JavaPrimitive.AddInstance.name()
                    acmd.ref = (adaptation.ref as Array<Any>).get(1)
                    adaptationModel.addAdaptations(acmd)

                    val uDiccmd = adaptationModelFactory.createAdaptationPrimitive()
                    uDiccmd.primitiveType = JavaPrimitive.UpdateDictionaryInstance.name()
                    uDiccmd.ref = (adaptation.ref as Array<Any>).get(1)
                    adaptationModel.addAdaptations(uDiccmd)

                    val startcmd = adaptationModelFactory.createAdaptationPrimitive()
                    startcmd.primitiveType = JavaPrimitive.StartInstance.name()
                    startcmd.ref = (adaptation.ref as Array<Any>).get(1)
                    adaptationModel.addAdaptations(startcmd)
                }
                else -> {
                }
            }
        }
        return adaptationModel;
    }

}
