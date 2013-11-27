/*

package org.kevoree.library.defaultNodeTypes.planning.scheduling

import org.kevoreeadaptation.AdaptationModel
import org.kevoreeadaptation.AdaptationPrimitive
import org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory
import org.jgrapht.alg.DirectedNeighborIndex
import java.util.ArrayList
import org.jgrapht.DirectedGraph
import org.kevoree.kompare.StepBuilder
import org.kevoreeadaptation.ParallelStep
import org.kevoreeadaptation.KevoreeAdaptationFactory

*/
/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 18/10/13
 * Time: 14:38
 *
 * @author Erwan Daubert
 * @version 1.0
 *//*


public class SchedulingAlgorithm:StepBuilder {
    override var currentSteps: ParallelStep? = null
    override var previousStep: ParallelStep? = null
    override var adaptationModelFactory: KevoreeAdaptationFactory = DefaultKevoreeAdaptationFactory()

    private val factory = DefaultKevoreeAdaptationFactory()

    fun schedule(adaptationModel : AdaptationModel) : AdaptationModel {
        var commands  = adaptationModel.adaptations
//        var stepList = Array[List[AdaptationPrimitive]]()
        if (commands.size > 1) {
            // build graph of dependencies between commands
            val graph = buildGraph(commands)
            val index = DirectedNeighborIndex(graph)
            var number = 0
            var alreadySeenList = ArrayList<AdaptationPrimitive>()
            var previousStep = graph.vertexSet().filter {v -> index.predecessorsOf(v). size () == 0}
            var step = factory.createParallelStep()
            var currentStep = step
            adaptationModel.orderedPrimitiveSet = currentStep
            // generate steps while all the commands are not set in a step
            while (number < graph.vertexSet().size()) {
                var newStep = ArrayList<AdaptationPrimitive>()
                previousStep.forEach {
                    p ->
                    // get all commands for which all predecessors have been set on previous steps
                    newStep.addAll(index.successorListOf(p).filter{v ->index.predecessorsOf(v).forAll{pred -> alreadySeenList.contains(pred)}})
                }
                // create a step with all the commands found
                currentStep.addAllAdaptations(newStep)
                step = factory.createParallelStep()
                currentStep.nextStep = step
                currentStep = step
                alreadySeenList.addAll(newStep)
                number = number + newStep.size
                previousStep = newStep
            }
        } else {
            createNextStep(commands)
        }
        adaptationModel
    }

    fun buildGraph(commands : jet.List<AdaptationPrimitive>) : DirectedGraph {

    }
}
*/
