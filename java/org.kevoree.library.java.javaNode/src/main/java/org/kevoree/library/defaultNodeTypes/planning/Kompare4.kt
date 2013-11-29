package org.kevoree.library.defaultNodeTypes.planning

import org.kevoreeadaptation.AdaptationModel
import org.kevoree.ContainerRoot
import org.kevoreeadaptation.impl.DefaultKevoreeAdaptationFactory
import org.kevoree.compare.DefaultModelCompare
import org.kevoree.Instance
import org.kevoreeadaptation.AdaptationPrimitive
import java.util.HashSet
import org.kevoree.modeling.api.trace.TraceSequence
import org.kevoree.Port
import org.kevoree.modeling.api.trace.ModelSetTrace
import org.kevoree.modeling.api.trace.ModelRemoveTrace
import org.kevoree.modeling.api.trace.ModelAddTrace
import org.kevoree.modeling.api.KMFContainer
import org.kevoree.modeling.api.util.ModelVisitor
import org.kevoree.DeployUnit
import org.kevoree.ContainerNode
import org.kevoree.modeling.api.trace.ModelTrace
import org.kevoree.Channel
import org.kevoree.library.defaultNodeTypes.ModelRegistry

public abstract class Kompare4(val registry: ModelRegistry) {

    private val modelCompare = DefaultModelCompare()
    private val adaptationModelFactory = DefaultKevoreeAdaptationFactory()

    /* Helper to create command */
    private fun adapt(primitive: JavaPrimitive, elem: Any?, model: ContainerRoot): AdaptationPrimitive {
        val ccmd = adaptationModelFactory.createAdaptationPrimitive()
        ccmd.primitiveType = primitive.name()
        ccmd.ref = elem
        return ccmd
    }

    data class TupleObjPrim(val obj: KMFContainer, val p: JavaPrimitive)

    open public fun compareModels(currentModel: ContainerRoot, targetModel: ContainerRoot, nodeName: String): AdaptationModel {
        val adaptationModel = adaptationModelFactory.createAdaptationModel()

        val elementAlreadyProcessed = HashSet<TupleObjPrim>()
        val currentNode = currentModel.findNodesByID(nodeName)
        val targetNode = targetModel.findNodesByID(nodeName)
        var traces: TraceSequence? = null

        fun fillAdditional() {
            for(n in targetNode!!.hosts){
                val previousNode = currentModel.findByPath(n.path()!!)
                if(previousNode != null){
                    traces!!.append(modelCompare.diff(previousNode, n))
                } else {
                    traces!!.populate(n.toTraces(true, true))
                }
            }
            for(g in targetNode.groups){
                val previousGroup = currentModel.findByPath(g.path()!!)
                if(previousGroup != null){
                    traces!!.append(modelCompare.diff(previousGroup, g))
                } else {
                    traces!!.populate(g.toTraces(true, true))
                }
            }
            //This process can really slow down
            for(comp in targetNode.components){
                fun fillPort(ports: List<Port>) {
                    for(port in ports){
                        for(b in port.bindings){
                            if(b.hub != null){
                                val previousChannel = currentModel.findByPath(b.hub!!.path()!!)
                                if(previousChannel != null){
                                    traces!!.append(modelCompare.diff(previousChannel, b.hub!!))
                                } else {
                                    traces!!.populate(b.hub!!.toTraces(true, true))
                                }
                            }
                        }
                    }
                }
                fillPort(comp.provided)
                fillPort(comp.required)
            }
        }

        if (currentNode != null && targetNode != null) {
            traces = modelCompare.diff(currentNode, targetNode)
            fillAdditional()
        } else {
            if(targetNode != null){
                traces = modelCompare.inter(targetNode, targetNode)
                fillAdditional()
            }
        }
        if (traces != null) {
            for(trace in traces!!.traces) {
                val modelElement = targetModel.findByPath(trace.srcPath)
                when(trace.refName) {
                    "components" -> {
                        if(trace.srcPath == targetNode!!.path()){
                            when(trace) {
                                is ModelAddTrace -> {
                                    val elemToAdd = targetModel.findByPath(trace.previousPath!!)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.AddInstance, elemToAdd, targetModel))
                                }
                                is ModelRemoveTrace -> {
                                    val elemToAdd = currentModel.findByPath(trace.objPath)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveInstance, elemToAdd, targetModel))
                                }
                            }
                        }
                    }
                    "hosts" -> {
                        if(trace.srcPath == targetNode!!.path()){
                            when(trace) {
                                is ModelAddTrace -> {
                                    val elemToAdd = targetModel.findByPath(trace.previousPath!!)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.AddInstance, elemToAdd, targetModel))
                                }
                                is ModelRemoveTrace -> {
                                    val elemToAdd = currentModel.findByPath(trace.objPath)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveInstance, elemToAdd, targetModel))
                                }
                            }
                        }
                    }
                    "groups" -> {
                        if(trace.srcPath == targetNode!!.path()){
                            when(trace) {
                                is ModelAddTrace -> {
                                    val elemToAdd = targetModel.findByPath(trace.previousPath!!)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.AddInstance, elemToAdd, targetModel))
                                }
                                is ModelRemoveTrace -> {
                                    val elemToAdd = currentModel.findByPath(trace.objPath)
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveInstance, elemToAdd, targetModel))
                                }
                            }
                        }
                    }
                    "bindings" -> {
                        if(!(targetModel.findByPath(trace.srcPath) is Channel)){
                            when(trace) {

                                is ModelAddTrace -> {
                                    val binding = targetModel.findByPath(trace.previousPath!!) as? org.kevoree.MBinding
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.AddBinding, binding, targetModel))
                                    val channel = binding?.hub
                                    if(channel != null && registry.lookup(channel) == null){
                                        if(!elementAlreadyProcessed.contains(TupleObjPrim(channel, JavaPrimitive.AddInstance))){
                                            adaptationModel.addAdaptations(adapt(JavaPrimitive.AddInstance, channel, targetModel))
                                            elementAlreadyProcessed.add(TupleObjPrim(channel, JavaPrimitive.AddInstance))
                                        }
                                    }
                                }
                                is ModelRemoveTrace -> {
                                    val binding = targetModel.findByPath(trace.objPath) as? org.kevoree.MBinding
                                    val previousBinding = currentModel.findByPath(trace.objPath) as? org.kevoree.MBinding
                                    val channel = binding?.hub
                                    var oldChannel = previousBinding?.hub
                                    //check if not no current usage of this channel
                                    var stillUsed: Boolean = (channel != null)
                                    if(channel != null){
                                        for(loopBinding in channel.bindings){
                                            if(loopBinding.port?.eContainer() == targetNode){
                                                stillUsed = true
                                            }
                                        }
                                    }
                                    if(!stillUsed && registry.lookup(oldChannel!!) != null){
                                        if(!elementAlreadyProcessed.contains(TupleObjPrim(channel!!, JavaPrimitive.RemoveInstance))){
                                            adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveInstance, channel, targetModel))
                                            elementAlreadyProcessed.add(TupleObjPrim(channel, JavaPrimitive.RemoveInstance))
                                        }
                                    }
                                    adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveBinding, binding, targetModel))
                                }
                            }
                        }

                    }
                    "started" -> {
                        if(modelElement is Instance && trace is ModelSetTrace){
                            if(trace.srcPath == targetNode!!.path()){
                                //HaraKiri case
                            } else {
                                if (trace.content?.toLowerCase() == "true") {
                                    if(!elementAlreadyProcessed.contains(TupleObjPrim(modelElement, JavaPrimitive.StartInstance))){
                                        adaptationModel.addAdaptations(adapt(JavaPrimitive.StartInstance, modelElement, targetModel))
                                        elementAlreadyProcessed.add(TupleObjPrim(modelElement, JavaPrimitive.StartInstance))
                                    }
                                } else {
                                    if(!elementAlreadyProcessed.contains(TupleObjPrim(modelElement, JavaPrimitive.StopInstance))){
                                        adaptationModel.addAdaptations(adapt(JavaPrimitive.StopInstance, modelElement, targetModel))
                                        elementAlreadyProcessed.add(TupleObjPrim(modelElement, JavaPrimitive.StopInstance))
                                    }
                                }
                            }
                        }
                    }
                    "typeDefinition" -> {
                        if(modelElement is Instance){
                            //TODO continuous design
                        }
                    }
                    "value" -> {
                        if(modelElement is org.kevoree.Dictionary){
                            if(!elementAlreadyProcessed.contains(TupleObjPrim(modelElement, JavaPrimitive.UpdateDictionaryInstance))){
                                adaptationModel.addAdaptations(adapt(JavaPrimitive.UpdateDictionaryInstance, modelElement, targetModel))
                                elementAlreadyProcessed.add(TupleObjPrim(modelElement, JavaPrimitive.UpdateDictionaryInstance))
                            }
                        }
                    }
                    else -> {
                    }
                }
                processTrace(trace, adaptationModel)
            }
        }
        var foundDeployUnitsToRemove = HashSet<String>()
        currentNode?.visit(object : ModelVisitor(){
            override fun visit(elem: KMFContainer, refNameInParent: String, parent: KMFContainer) {
                if(elem is DeployUnit){
                    foundDeployUnitsToRemove.add(elem.path()!!)
                }
                //optimization purpose
                if( (elem is ContainerNode && elem != currentNode)){
                    noChildrenVisit()
                    noReferencesVisit()
                }
            }

        }, true, true, true)
        targetNode?.visit(object : ModelVisitor(){
            override fun visit(elem: KMFContainer, refNameInParent: String, parent: KMFContainer) {
                if(elem is DeployUnit){
                    if(registry.lookup(elem) == null){
                        adaptationModel.addAdaptations(adapt(JavaPrimitive.AddDeployUnit, elem, targetModel))
                        adaptationModel.addAdaptations(adapt(JavaPrimitive.LinkDeployUnit, elem, targetModel))
                    }
                    foundDeployUnitsToRemove.remove(elem.path()!!)
                }
                //optimization purpose
                if( (elem is ContainerNode && elem != currentNode) ){
                    noChildrenVisit()
                    noReferencesVisit()
                }
            }
        }, true, true, true)
        for(pathDeployUnitToDrop in foundDeployUnitsToRemove){
            adaptationModel.addAdaptations(adapt(JavaPrimitive.RemoveDeployUnit, currentModel.findByPath(pathDeployUnitToDrop), targetModel))
        }
        return adaptationModel
    }

    open fun processTrace(trace: ModelTrace, adaptationModel: AdaptationModel) {

    }

}