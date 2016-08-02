package org.kevoree.library.java.planning;

import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.adaptation.AdaptationType;
import org.kevoree.api.adaptation.Step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 21/09/11
 * Time: 17:54
 */

public abstract class KevoreeScheduler {

    public AdaptationModel schedule(AdaptationModel adaptionModel) {
        if (!adaptionModel.getAdaptations().isEmpty()) {
            HashMap<String, List<AdaptationPrimitive>> classedAdaptations = classify(adaptionModel.getAdaptations());
            adaptionModel.setOrderedPrimitiveSet(createStep(classedAdaptations.get(AdaptationType.AddDeployUnit.name()),AdaptationType.AddDeployUnit));
            Step firstStep = adaptionModel.getOrderedPrimitiveSet();
            firstStep.next(createStep(classedAdaptations.get(AdaptationType.StopInstance.name()),AdaptationType.StopInstance))
                    .next(createStep(classedAdaptations.get(AdaptationType.RemoveBinding.name()),AdaptationType.RemoveBinding))
                    .next(createStep(classedAdaptations.get(AdaptationType.RemoveInstance.name()),AdaptationType.RemoveInstance))
                    .next(createStep(classedAdaptations.get(AdaptationType.RemoveDeployUnit.name()),AdaptationType.RemoveDeployUnit))
                    .next(createStep(classedAdaptations.get(AdaptationType.AddInstance.name()),AdaptationType.AddInstance))
                    .next(createStep(classedAdaptations.get(AdaptationType.AddBinding.name()),AdaptationType.AddBinding))
                    .next(createStep(classedAdaptations.get(AdaptationType.UpdateDictionaryInstance.name()),AdaptationType.UpdateDictionaryInstance))
                    .next(createStep(classedAdaptations.get(AdaptationType.UpdateCallMethod.name()),AdaptationType.UpdateCallMethod))
                    .next(createStep(classedAdaptations.get(AdaptationType.StartInstance.name()),AdaptationType.StartInstance));
        } else {
            adaptionModel.setOrderedPrimitiveSet(null);
        }
        return adaptionModel;
    }

    private HashMap<String, List<AdaptationPrimitive>> classify(List<AdaptationPrimitive> inputs) {
        HashMap<String, List<AdaptationPrimitive>> result = new HashMap<String, List<AdaptationPrimitive>>();
        for (AdaptationPrimitive adapt : inputs) {
            List<AdaptationPrimitive> l;
            if (!result.containsKey(adapt.getPrimitiveType())) {
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
        Step step = new Step();
        step.setAdaptationType(type);
        if (commands != null) {
            step.getAdaptations().addAll(commands);
        }
        return step;
    }
}