package org.kevoree.library.defaultNodeTypes.planning

import org.kevoreeadaptation.AdaptationModel
import org.kevoreeadaptation.AdaptationPrimitive
import java.util.ArrayList
import org.kevoree.library.defaultNodeTypes.planning.scheduling.SchedulingWithTopologicalOrderAlgo

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 21/09/11
 * Time: 17:54
 */

trait KevoreeScheduler : StepBuilder {

    open fun schedule(adaptionModel: AdaptationModel, nodeName: String): AdaptationModel {
        if (!adaptionModel.adaptations.isEmpty()) {

            adaptationModelFactory = org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory()
            val scheduling = SchedulingWithTopologicalOrderAlgo()

            nextStep()
            adaptionModel.orderedPrimitiveSet = currentSteps
            //STOP INSTANCEs
            var stepToInsert = scheduling.schedule(adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.StopInstance.name() }, false)
            if (stepToInsert != null && !stepToInsert!!.adaptations.isEmpty()) {
                insertStep(stepToInsert!!)
            }

            // REMOVE BINDINGS
            createNextStep(JavaPrimitive.RemoveBinding, adaptionModel.adaptations.filter { adapt -> (adapt.primitiveType == JavaPrimitive.RemoveBinding.name() ) })

            // REMOVE INSTANCEs
            createNextStep(JavaPrimitive.RemoveInstance, adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.RemoveInstance.name() })

            // REMOVE DEPLOYUNITs
            createNextStep(JavaPrimitive.RemoveDeployUnit, adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.RemoveDeployUnit.name() })

            // UPDATE DEPLOYUNITs
            createNextStep(JavaPrimitive.UpdateDeployUnit, adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.UpdateDeployUnit.name() })

            // ADD DEPLOYUNITs
            createNextStep(JavaPrimitive.AddDeployUnit, adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.AddDeployUnit.name() })

            // ADD INSTANCEs
            // createNextStep(JavaPrimitive.AddInstance, adaptionModel.adaptations.filter{ adapt -> adapt.primitiveType!!.name == JavaSePrimitive.AddInstance })

            adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.AddInstance.name() }.forEach {
                addInstance ->
                val list = ArrayList<AdaptationPrimitive>()
                list.add(addInstance)
                createNextStep(JavaPrimitive.AddInstance, list)
            }

            // ADD BINDINGs
            createNextStep(JavaPrimitive.AddBinding, adaptionModel.adaptations.filter { adapt -> (adapt.primitiveType == JavaPrimitive.AddBinding.name() ) })

            // UPDATE DICTIONARYs
            createNextStep(JavaPrimitive.UpdateDictionaryInstance, adaptionModel.adaptations.filter { adapt -> adapt.primitiveType == JavaPrimitive.UpdateDictionaryInstance.name() })

            // START INSTANCEs
            stepToInsert = scheduling.schedule(adaptionModel.adaptations.filter {
                adapt ->
                adapt.primitiveType == JavaPrimitive.StartInstance.name()
            }, true)
            if (stepToInsert != null && !stepToInsert!!.adaptations.isEmpty()) {
                insertStep(stepToInsert!!)
            }
        } else {
            adaptionModel.orderedPrimitiveSet = null
        }
        clearSteps()
        return adaptionModel
    }
}