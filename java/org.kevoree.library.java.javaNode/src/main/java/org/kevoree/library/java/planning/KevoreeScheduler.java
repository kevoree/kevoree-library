package org.kevoree.library.java.planning;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.kevoree.factory.KevoreeFactory;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.Step;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.adaptation.SequentialStep;


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 21/09/11
 * Time: 17:54
 */

public abstract class KevoreeScheduler {

    private KevoreeFactory adaptationModelFactory;

    public AdaptationModel schedule(AdaptationModel adaptionModel, String nodeName) {
        if (!adaptionModel.getAdaptations().isEmpty()) {
            adaptationModelFactory = new DefaultKevoreeFactory();
            HashMap<String, List<AdaptationPrimitive>> classedAdaptations = classify(adaptionModel.getAdaptations());
            adaptionModel.setOrderedPrimitiveSet(createStep(classedAdaptations.get(JavaPrimitive.AddDeployUnit.name())));
            Step currentStep = adaptionModel.getOrderedPrimitiveSet();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.LinkDeployUnit.name())));
            currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.AddInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.StopInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.RemoveBinding.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.UpgradeInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.RemoveInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.AddBinding.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.UpdateDictionaryInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.UpdateCallMethod.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.StartInstance.name())));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(JavaPrimitive.RemoveDeployUnit.name())));
        } else {
            adaptionModel.setOrderedPrimitiveSet(null);
        }
        return adaptionModel;
    }

    private HashMap<String, List<AdaptationPrimitive>> classify(List<AdaptationPrimitive> inputs) {
        HashMap<String, List<AdaptationPrimitive>> result = new HashMap<String, List<AdaptationPrimitive>>();
        for(AdaptationPrimitive adapt : inputs) {
            List<AdaptationPrimitive> l = null;
            if(!result.containsKey(adapt.getPrimitiveType())){
                l = new ArrayList<AdaptationPrimitive>();
                result.put(adapt.getPrimitiveType(), l);
            } else {
                l = result.get(adapt.getPrimitiveType());
            }
            l.add(adapt);

        }
        return result;
    }

    public Step createStep(List<AdaptationPrimitive> commands) {
      //  var currentSteps = adaptationModelFactory.createParallelStep()
        SequentialStep currentSteps = new SequentialStep();
        if(commands != null){
            currentSteps.getAdaptations().addAll(commands);
        }
        return currentSteps;
    }


}