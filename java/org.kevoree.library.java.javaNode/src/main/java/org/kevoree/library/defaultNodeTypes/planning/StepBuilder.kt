package org.kevoree.library.defaultNodeTypes.planning

import org.kevoreeadaptation.AdaptationPrimitive
import org.kevoreeadaptation.KevoreeAdaptationFactory
import org.kevoreeadaptation.ParallelStep

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 01/08/13
 * Time: 17:33
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public trait StepBuilder {
    var currentSteps: ParallelStep?
    var previousStep: ParallelStep?
    var adaptationModelFactory: KevoreeAdaptationFactory

    public fun nextStep() {
        if(currentSteps == null){
            currentSteps = adaptationModelFactory.createParallelStep()
        }
        if (!currentSteps!!.adaptations.isEmpty()) {
            previousStep = currentSteps
            currentSteps = adaptationModelFactory.createParallelStep()
            previousStep!!.nextStep = currentSteps
        }
    }

    public fun clearSteps() {
        currentSteps = null
        previousStep = null
    }

    public open fun createNextStep(commands: List<AdaptationPrimitive>) {
        if (!commands.isEmpty()) {
            if (currentSteps == null) {
                nextStep()
            }
            currentSteps!!.addAllAdaptations(commands)
            nextStep()
        }
    }


}