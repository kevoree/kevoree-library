package org.kevoree.library.defaultNodeTypes.planning

import java.util.HashMap
import java.util.ArrayList

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 21/09/11
 * Time: 17:54
 */

trait KevoreeScheduler {

    var adaptationModelFactory: KevoreeAdaptationFactory

    open fun schedule(adaptionModel: AdaptationModel, nodeName: String): AdaptationModel {
        if (!adaptionModel.adaptations.isEmpty()) {
            adaptationModelFactory = org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory()
            val classedAdaptations = classify(adaptionModel.adaptations)
            adaptionModel.orderedPrimitiveSet = createStep(classedAdaptations.get(JavaPrimitive.AddDeployUnit.name()))
            var currentStep = adaptionModel.orderedPrimitiveSet
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.LinkDeployUnit.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.AddInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.StopInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.RemoveBinding.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.RemoveInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.AddBinding.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.UpdateDictionaryInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.StartInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.RemoveDeployUnit.name()))
        } else {
            adaptionModel.orderedPrimitiveSet = null
        }
        return adaptionModel
    }

    private fun classify(inputs: List<AdaptationPrimitive>): HashMap<String, MutableList<AdaptationPrimitive>> {
        var result = HashMap<String, MutableList<AdaptationPrimitive>>()
        for(adapt in inputs){
            var l: MutableList<AdaptationPrimitive>? = null
            if(!result.containsKey(adapt.primitiveType)){
                l = ArrayList<AdaptationPrimitive>()
                result.put(adapt.primitiveType!!, l!!)
            } else {
                l = result.get(adapt.primitiveType!!)
            }
            l!!.add(adapt)

        }
        return result
    }

    public fun createStep(commands: MutableList<AdaptationPrimitive>?): ParallelStep {
        var currentSteps = adaptationModelFactory.createParallelStep()
        if(commands != null){
            currentSteps.addAllAdaptations(commands)
        }
        return currentSteps
    }


}