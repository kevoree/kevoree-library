package org.kevoree.library.java.planning;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.kevoree.api.adaptation.*;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.factory.DefaultKevoreeFactory;


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
            adaptionModel.setOrderedPrimitiveSet(createStep(classedAdaptations.get(AdaptationType.AddDeployUnit.name()),AdaptationType.AddDeployUnit));
            Step currentStep = adaptionModel.getOrderedPrimitiveSet();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.LinkDeployUnit.name()),AdaptationType.LinkDeployUnit));
            currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.AddInstance.name()),AdaptationType.AddInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.StopInstance.name()),AdaptationType.StopInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.RemoveBinding.name()),AdaptationType.RemoveBinding));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.UpgradeInstance.name()),AdaptationType.UpgradeInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.RemoveInstance.name()),AdaptationType.RemoveInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.AddBinding.name()),AdaptationType.AddBinding));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.UpdateDictionaryInstance.name()),AdaptationType.UpdateDictionaryInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.UpdateCallMethod.name()),AdaptationType.UpdateCallMethod));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.StartInstance.name()),AdaptationType.StartInstance));
           currentStep = currentStep.getNextStep();
            currentStep.setNextStep(createStep(classedAdaptations.get(AdaptationType.RemoveDeployUnit.name()),AdaptationType.RemoveDeployUnit));
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

    public Step createStep(List<AdaptationPrimitive> commands, AdaptationType type) {
      //  var currentSteps = adaptationModelFactory.createParallelStep()
        Step currentSteps = new Step();
        currentSteps.setAdaptationType(type);
        if(commands != null){
            currentSteps.getAdaptations().addAll(commands);
        }
        return currentSteps;
    }


}