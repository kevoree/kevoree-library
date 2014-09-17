package org.kevoree.library.java.planning

import java.util.HashMap
import java.util.ArrayList
import org.kevoree.factory.KevoreeFactory
import org.kevoree.factory.DefaultKevoreeFactory
import org.kevoree.api.adaptation.AdaptationModel
import org.kevoree.api.adaptation.Step
import org.kevoree.api.adaptation.AdaptationPrimitive
import org.kevoree.api.adaptation.SequentialStep


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 21/09/11
 * Time: 17:54
 */

trait KevoreeScheduler {

    var adaptationModelFactory: KevoreeFactory

    open fun schedule(adaptionModel: AdaptationModel, nodeName: String): AdaptationModel {
        if (!adaptionModel.adaptations.isEmpty()) {
            adaptationModelFactory = DefaultKevoreeFactory()
            val classedAdaptations = classify(adaptionModel.adaptations)
            adaptionModel.orderedPrimitiveSet = createStep(classedAdaptations.get(JavaPrimitive.AddDeployUnit.name()))
            var currentStep : Step? = adaptionModel.orderedPrimitiveSet
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.LinkDeployUnit.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.AddInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.StopInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.RemoveBinding.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.UpgradeInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.RemoveInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.AddBinding.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.UpdateDictionaryInstance.name()))
            currentStep = currentStep!!.nextStep
            currentStep!!.nextStep = createStep(classedAdaptations.get(JavaPrimitive.UpdateCallMethod.name()))
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

    public fun createStep(commands: MutableList<AdaptationPrimitive>?): Step {
      //  var currentSteps = adaptationModelFactory.createParallelStep()
        var currentSteps = SequentialStep()
        if(commands != null){
            currentSteps.adaptations.addAll(commands)
        }
        return currentSteps
    }


}