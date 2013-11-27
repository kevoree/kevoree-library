package org.kevoree.library.defaultNodeTypes.planning.scheduling

import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.traverse.TopologicalOrderIterator
import org.kevoree.*
import java.util.ArrayList
import org.kevoreeadaptation.AdaptationPrimitive
import java.util.HashMap
import org.kevoreeadaptation.ParallelStep
import org.kevoreeadaptation.KevoreeAdaptationFactory
import org.kevoree.framework.kaspects.PortAspect
import org.kevoree.library.defaultNodeTypes.planning.StepBuilder
import org.kevoree.library.defaultNodeTypes.planning.JavaPrimitive

class SchedulingWithTopologicalOrderAlgo : StepBuilder {
    override var previousStep: ParallelStep? = null
    override var currentSteps: ParallelStep? = null
    override var adaptationModelFactory: KevoreeAdaptationFactory = org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory()

    fun schedule(commands: List<AdaptationPrimitive>, start: Boolean): /*List<AdaptationPrimitive>*/ParallelStep? {
        clearSteps()
        nextStep()
        val firstStep = currentSteps
        if (commands.size > 1) {
            val graph = buildGraph(commands, start)
            val topologicAlgorithm = TopologicalOrderIterator(graph)

            while (topologicAlgorithm.hasNext()) {
                val element = topologicAlgorithm.next()
                val list = ArrayList<AdaptationPrimitive>()
                list.add(element)
                if (start) {
                    createNextStep(JavaPrimitive.StartInstance, list)
                } else {
                    createNextStep(JavaPrimitive.StopInstance, list)
                }
                nextStep()
            }
        } else if (!commands.isEmpty()) {
            if (start) {
                createNextStep(JavaPrimitive.StartInstance, commands)
            } else {
                createNextStep(JavaPrimitive.StopInstance, commands)
            }
        }
        return firstStep;
    }


    private class Assoc2<E, G>(_1: E, _2: G){}


    /**
     * each command is a vertex and edges represent dependency order between commands
     */
    private fun buildGraph(commands: List<AdaptationPrimitive>,
                           start: Boolean): DefaultDirectedGraph<AdaptationPrimitive, Assoc2<AdaptationPrimitive, AdaptationPrimitive>> {

        val clazz = Assoc2<AdaptationPrimitive, AdaptationPrimitive>(commands.get(0), commands.get(0)).javaClass

        val graph = DefaultDirectedGraph<AdaptationPrimitive, Assoc2<AdaptationPrimitive, AdaptationPrimitive>>(clazz)

        //        val map = lookForPotentialConstraints(commands)
        val map = lookForPotentialConstraints(commands)

        for (command in commands) {
            for (command2 in commands) {
                graph.addVertex(command2)
                if (!command.equals(command2)) {
                    val cmdDep = (map.get(command2.ref as Instance))
                    if(cmdDep != null){
                        if (cmdDep.contains(command.ref as Instance)) {
                            if (start) {
                                graph.addEdge(command2, command, Assoc2(command2, command))
                            } else {
                                graph.addEdge(command, command2, Assoc2(command, command2))
                            }
                        }
                    }
                }
            }
        }
        return graph
    }

    /*
       * Return Map
       *
       * Instance Map key is a dependency of all List instances return by key
       *
       *
       * [i:Instance,li:List[Instance]]
       * li depends i
       *
       *
       * */
    private fun lookForPotentialConstraints(commands: List<AdaptationPrimitive>): Map<Instance, List<Instance>> {
        val instanceDependencies: HashMap<Instance, MutableList<Instance>> = HashMap<Instance, MutableList<Instance>>()

        for (command in commands) {
            if(command.ref is ComponentInstance) {
                val component = command.ref as ComponentInstance
                var instancesDependencyForComponent: MutableList<Instance>? = instanceDependencies.get(component)
                if(instancesDependencyForComponent == null){
                    instancesDependencyForComponent = ArrayList<Instance>()
                }

                // Looking for channel that send message to the instance
                for (port in component.provided) {
                    for (binding in port.bindings) {
                        // the channel is a dependency of the instance (must be start after and stop before the instance)
                        val channel = binding.hub!!
                        if (!instancesDependencyForComponent!!.contains(channel)) {
                            var instancesDependencyForChannel: MutableList<Instance>? = instanceDependencies.get(channel)
                            if(instancesDependencyForChannel == null){
                                instancesDependencyForChannel = ArrayList<Instance>()
                            }
                            for (bindingFromChannel in channel.bindings) {
                                val portFromBinding = bindingFromChannel.port!!
                                // each required port connected to the channel is host by a component that is a dependency of the instance (must be start after and stop before the instance) and a dependency of the channel
                                if (PortAspect().isRequiredPort(bindingFromChannel.port!!) && !portFromBinding.portTypeRef!!.noDependency!! && !instancesDependencyForChannel!!.contains(bindingFromChannel.port!!.eContainer() as Instance)) {
                                    //                                    System.out.println((portFromBinding.eContainer() as NamedElement).getName() + "\t" + portFromBinding.getPortTypeRef()!!.getName() + "\t" + portFromBinding.getPortTypeRef()!!.getNoDependency())
                                    //                                    System.out.println(component.getName() + " -> " + channel.getName())
                                    instancesDependencyForComponent!!.add(channel)

                                    //                                    System.out.println(component.getName() + " -> " + (bindingFromChannel.getPort()!!.eContainer() as NamedElement).getName())
                                    //                                    System.out.println(channel.getName() + " -> " + (bindingFromChannel.getPort()!!.eContainer() as NamedElement).getName())
                                    instancesDependencyForComponent!!.add(portFromBinding.eContainer() as Instance)
                                    instancesDependencyForChannel!!.add(portFromBinding.eContainer() as Instance)
                                }
                            }
                            instanceDependencies.put(channel, instancesDependencyForChannel!!)
                        }
                    }
                }
                instanceDependencies.put(component, instancesDependencyForComponent!!)
            }
        }
        return instanceDependencies
    }

}
