package org.kevoree.library.java.planning;

import org.kevoree.*;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.trace.ModelAddTrace;
import org.kevoree.pmodeling.api.trace.ModelRemoveTrace;
import org.kevoree.pmodeling.api.trace.ModelSetTrace;

import org.kevoree.pmodeling.api.trace.ModelTrace;
import org.kevoree.pmodeling.api.trace.TraceSequence;
import org.kevoree.pmodeling.api.util.ModelVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by duke on 9/26/14.
 */
public class KevoreeKompareBean extends KevoreeScheduler {

    public KevoreeKompareBean(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    private ModelRegistry modelRegistry;

    KevoreeFactory adaptationModelFactory = new DefaultKevoreeFactory();
    ModelCompare modelCompare = adaptationModelFactory.createModelCompare();

    public AdaptationModel plan(ContainerRoot actualModel, ContainerRoot targetModel, String nodeName) {
        AdaptationModel adaptationModel = compareModels(actualModel, targetModel, nodeName);
        AdaptationModel afterPlan = schedule(adaptationModel, nodeName);
        return afterPlan;
    }


    /* Helper to create command */
    private AdaptationPrimitive adapt(JavaPrimitive primitive, Object elem) {
        AdaptationPrimitive ccmd = new AdaptationPrimitive();
        ccmd.setPrimitiveType(primitive.name());
        ccmd.setRef(elem);
        return ccmd;
    }

    private class TupleObjPrim {
        private KMFContainer obj;

        private TupleObjPrim(KMFContainer obj, JavaPrimitive p) {
            this.obj = obj;
            this.p = p;
        }

        public JavaPrimitive getP() {
            return p;
        }

        public void setP(JavaPrimitive p) {
            this.p = p;
        }

        public KMFContainer getObj() {
            return obj;
        }

        public void setObj(KMFContainer obj) {
            this.obj = obj;
        }

        private JavaPrimitive p;

    }

    public void processTrace(ModelTrace trace, AdaptationModel adaptationModel) {
        if (Log.TRACE) {
            Log.trace(trace.toString());
        }
    }

    private List<ModelTrace> deepToTrace(KMFContainer elem, final String currentNodeName) {
        final List<ModelTrace> result = new ArrayList<ModelTrace>();
        result.addAll(elem.toTraces(true, true));
        elem.visit(new ModelVisitor() {
            public void visit(KMFContainer child, String refNameInParent, KMFContainer parent) {
                if (child instanceof ContainerNode && !((ContainerNode) child).getName().equals(currentNodeName)) {
                    noChildrenVisit();
                    noReferencesVisit();
                    //protection but should not be std case
                } else {
                    result.addAll(child.toTraces(true, true));
                }
            }
        }, true, true, false);
        return result;
    }

    public void fillAdditional(TraceSequence traces, ContainerNode targetNode, ContainerRoot currentModel, String nodeName) {
        for (ContainerNode n : targetNode.getHosts()) {
            ContainerNode previousNode = (ContainerNode) currentModel.findByPath(n.path());
            if (previousNode != null) {
                traces.populate(previousNode.createTraces(n, false, false, false, true));
            } else {
                traces.populate(n.toTraces(true, true));
            }
        }
        for (Group g : targetNode.getGroups()) {
            Group previousGroup = (Group) currentModel.findByPath(g.path());
            if (previousGroup != null) {
                traces.append(modelCompare.diff(previousGroup, g));
            } else {
                traces.populate(deepToTrace(g, nodeName));
            }
        }
        //This process can really slow down
        HashSet<String> channelsAlreadySeen = new HashSet<String>();
        for (ComponentInstance comp : targetNode.getComponents()) {
            for (Port port : comp.getProvided()) {
                for (MBinding b : port.getBindings()) {
                    if (b.getHub() != null && !channelsAlreadySeen.contains(b.getHub().path())) {
                        Channel previousChannel = (Channel) currentModel.findByPath(b.getHub().path());
                        if (previousChannel != null) {
                            traces.append(modelCompare.diff(previousChannel, b.getHub()));
                        } else {
                            traces.populate(deepToTrace(b.getHub(), nodeName));
                        }
                        channelsAlreadySeen.add(b.getHub().path());
                    }
                }
            }
            for (Port port : comp.getRequired()) {
                for (MBinding b : port.getBindings()) {
                    if (b.getHub() != null && !channelsAlreadySeen.contains(b.getHub().path())) {
                        Channel previousChannel = (Channel) currentModel.findByPath(b.getHub().path());
                        if (previousChannel != null) {
                            traces.append(modelCompare.diff(previousChannel, b.getHub()));
                        } else {
                            traces.populate(deepToTrace(b.getHub(), nodeName));
                        }
                        channelsAlreadySeen.add(b.getHub().path());
                    }
                }
            }
        }
    }


    public AdaptationModel compareModels(ContainerRoot currentModel, ContainerRoot targetModel, String nodeName) {
        final AdaptationModel adaptationModel = new AdaptationModel();
        HashSet<TupleObjPrim> elementAlreadyProcessed = new HashSet<TupleObjPrim>();
        final ContainerNode currentNode = currentModel.findNodesByID(nodeName);
        ContainerNode targetNode = targetModel.findNodesByID(nodeName);
        TraceSequence traces = null;
        if (currentNode != null && targetNode != null) {
            traces = modelCompare.diff(currentNode, targetNode);
            fillAdditional(traces, targetNode, currentModel, nodeName);
        } else {
            if (targetNode != null) {
                traces = new TraceSequence(adaptationModelFactory);
                traces.populate(deepToTrace(targetNode, nodeName));
                fillAdditional(traces, targetNode, currentModel, nodeName);
            }
        }
        if (traces != null) {
            for (ModelTrace trace : traces.getTraces()) {
                KMFContainer modelElement = targetModel.findByPath(trace.getSrcPath());
                if (trace.getRefName().equals("components")) {
                    if (trace.getSrcPath().equals(targetNode.path())) {
                        if (trace instanceof ModelAddTrace) {
                            KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddInstance, elemToAdd));
                        }
                        if (trace instanceof ModelRemoveTrace) {
                            KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveInstance, elemToAdd));
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StopInstance, elemToAdd));
                        }
                    }
                }
                if (trace.getRefName().equals("hosts")) {
                    if (trace.getSrcPath().equals(targetNode.path())) {
                        if (trace instanceof ModelAddTrace) {
                            KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddInstance, elemToAdd));
                        }
                        if (trace instanceof ModelRemoveTrace) {
                            KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveInstance, elemToAdd));
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StopInstance, elemToAdd));
                        }
                    }
                }
                if (trace.getRefName().equals("groups")) {
                    if (trace.getSrcPath().equals(targetNode.path())) {
                        if (trace instanceof ModelAddTrace) {
                            KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddInstance, elemToAdd));
                        }
                        if (trace instanceof ModelRemoveTrace) {
                            KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveInstance, elemToAdd));
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StopInstance, elemToAdd));
                        }
                    }
                }
                if (trace.getRefName().equals("bindings")) {
                    if (!(targetModel.findByPath(trace.getSrcPath()) instanceof Channel)) {
                        if (trace instanceof ModelAddTrace) {
                            MBinding binding = (MBinding) targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddBinding, binding));
                            Channel channel = binding.getHub();
                            if (channel != null && modelRegistry.lookup(channel) == null) {
                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(channel, JavaPrimitive.AddInstance))) {
                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddInstance, channel));
                                    elementAlreadyProcessed.add(new TupleObjPrim(channel, JavaPrimitive.AddInstance));
                                }
                            }
                        }
                        if (trace instanceof ModelRemoveTrace) {
                            org.kevoree.MBinding binding = (org.kevoree.MBinding) currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                            org.kevoree.MBinding previousBinding = (org.kevoree.MBinding) currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                            Channel channel = binding.getHub();
                            Channel oldChannel = previousBinding.getHub();
                            //check if not no current usage of this channel
                            boolean stillUsed = channel != null;
                            if (channel != null) {
                                for (MBinding loopBinding : channel.getBindings()) {
                                    if (loopBinding.getPort() != null) {
                                        if (loopBinding.getPort().eContainer().equals(targetNode)) {
                                            stillUsed = true;
                                        }
                                    }
                                }
                            }
                            if (!stillUsed && modelRegistry.lookup(oldChannel) != null) {
                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(oldChannel, JavaPrimitive.RemoveInstance))) {
                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveInstance, oldChannel));
                                    elementAlreadyProcessed.add(new TupleObjPrim(oldChannel, JavaPrimitive.RemoveInstance));
                                    elementAlreadyProcessed.add(new TupleObjPrim(oldChannel, JavaPrimitive.StopInstance));
                                }
                            }
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                        }
                    }
                }
                if (trace.getRefName().equals("started")) {
                    if (modelElement instanceof Instance && trace instanceof ModelSetTrace) {
                        if (modelElement.eContainer() instanceof ContainerNode && !modelElement.eContainer().path().equals(targetNode.path())) {
                            //ignore it, for another node
                        } else {
                            if (trace.getSrcPath().equals(targetNode.path())) {
                                //HaraKiri case
                            } else {
                                if ( ((ModelSetTrace)trace).getContent().toLowerCase().equals("true")) {
                                    if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.StartInstance))) {
                                        adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StartInstance, modelElement));
                                        elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.StartInstance));
                                    }
                                } else {
                                    if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.StopInstance))) {
                                        adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StopInstance, modelElement));
                                        elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.StopInstance));
                                    }
                                }
                            }
                        }
                    }
                }
                if (trace.getRefName().equals("typeDefinition")) {
                    if (trace instanceof ModelAddTrace) {
                        if (modelElement instanceof Instance) {
                            Instance currentModelElement = (Instance) currentModel.findByPath(modelElement.path());
                            Instance targetModelElement = (Instance) targetModel.findByPath(modelElement.path());
                            if (currentModelElement != null && targetModelElement != null) {
                                //HaraKiri upgrade
                                if (modelElement.path().equals(targetNode.path())) {
                                    //Serious HaraKiri, should stop the platform and everything .... call the core to rebootstrap
                                } else {
                                    //upgrade internally
                                    if (currentModelElement.getStarted() == true && targetModelElement.getStarted() == true) {
                                        if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.StopInstance))) {
                                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StopInstance, modelElement));
                                            elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.StopInstance));
                                        }
                                    }
                                    //unbind
                                    if (currentModelElement instanceof Channel) {
                                        for (MBinding binding : ((Channel) currentModelElement).getBindings()) {
                                            if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                            }
                                        }
                                    } else {
                                        if (currentModelElement instanceof ComponentInstance) {
                                            for (Port binding : ((ComponentInstance) currentModelElement).getRequired()) {
                                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                                }
                                            }
                                            for (Port binding : ((ComponentInstance) currentModelElement).getProvided()) {
                                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                                }
                                            }
                                        }
                                    }
                                    if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.UpgradeInstance))) {
                                        adaptationModel.getAdaptations().add(adapt(JavaPrimitive.UpgradeInstance, modelElement));
                                        elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.UpgradeInstance));
                                    }
                                    //reinject dictionary
                                    List<Value> dicValues = targetModelElement.getDictionary().getValues();
                                    if (dicValues != null) {
                                        for (Value dicValue : dicValues) {
                                            Object[] values = new Object[]{modelElement, dicValue};
                                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.UpdateDictionaryInstance, values));
                                            elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.UpdateDictionaryInstance));
                                        }
                                    }
                                    //rebind
                                    if (targetModelElement instanceof Channel) {
                                        for (MBinding binding : ((Channel) targetModelElement).getBindings()) {
                                            if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                            }
                                        }
                                    } else {
                                        if (targetModelElement instanceof ComponentInstance) {
                                            for (Port binding : ((ComponentInstance) targetModelElement).getRequired()) {
                                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                                }
                                            }
                                            for (Port binding : ((ComponentInstance) targetModelElement).getProvided()) {
                                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(binding, JavaPrimitive.RemoveBinding))) {
                                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveBinding, binding));
                                                }
                                            }
                                        }
                                    }
                                    //restart
                                    if (currentModelElement.getStarted() == true && targetModelElement.getStarted() == true) {
                                        if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.StartInstance))) {
                                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.StartInstance, modelElement));
                                            elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.StartInstance));
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                if (trace.getRefName().equals("value")) {
                    if (modelElement instanceof org.kevoree.Value && modelElement.getRefInParent().equals("values")) {
                        Instance parentInstance = (Instance) modelElement.eContainer().eContainer();
                        if (parentInstance != null && parentInstance instanceof ContainerNode && parentInstance.getName().equals(nodeName) && currentNode == null) {
                            //noop
                        } else {

                            KMFContainer dictionaryParent = modelElement.eContainer();
                            if (dictionaryParent != null && dictionaryParent instanceof FragmentDictionary && !((FragmentDictionary) dictionaryParent).getName().equals(nodeName)) {

                            } else {
                                if (!elementAlreadyProcessed.contains(new TupleObjPrim(modelElement, JavaPrimitive.UpdateDictionaryInstance))) {
                                    Object[] values = new Object[]{modelElement.eContainer().eContainer(), modelElement};
                                    adaptationModel.getAdaptations().add(adapt(JavaPrimitive.UpdateDictionaryInstance, values));
                                    elementAlreadyProcessed.add(new TupleObjPrim(modelElement, JavaPrimitive.UpdateDictionaryInstance));
                                }
                                if (parentInstance != null) {
                                    if (!elementAlreadyProcessed.contains(new TupleObjPrim(parentInstance, JavaPrimitive.UpdateCallMethod))) {
                                        adaptationModel.getAdaptations().add(adapt(JavaPrimitive.UpdateCallMethod, parentInstance));
                                        elementAlreadyProcessed.add(new TupleObjPrim(parentInstance, JavaPrimitive.UpdateCallMethod));
                                    }
                                }
                            }
                        }
                    }
                }
                processTrace(trace, adaptationModel);
            }
        }
        final HashSet<String> foundDeployUnitsToRemove = new HashSet<String>();
        if (currentNode != null) {
            currentNode.visit(new ModelVisitor() {
                public void visit(KMFContainer elem, String refNameInParent, KMFContainer parent) {
                    if (elem instanceof DeployUnit) {
                        foundDeployUnitsToRemove.add(elem.path());
                    }
                    //optimization purpose
                    if ((elem instanceof ContainerNode && elem != currentNode)) {
                        noChildrenVisit();
                        noReferencesVisit();
                    }
                }

            }, true, true, true);
        }
        if (targetNode != null) {
            targetNode.visit(new ModelVisitor() {
                public void visit(KMFContainer elem, String refNameInParent, KMFContainer parent) {
                    if (elem instanceof DeployUnit) {
                        if (modelRegistry.lookup(elem) == null) {
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.AddDeployUnit, elem));
                            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.LinkDeployUnit, elem));
                        }
                        foundDeployUnitsToRemove.remove(elem.path());
                    }
                    //optimization purpose
                    if ((elem instanceof ContainerNode && elem != currentNode)) {
                        noChildrenVisit();
                        noReferencesVisit();
                    }
                }
            }, true, true, true);
        }
        for (String pathDeployUnitToDrop : foundDeployUnitsToRemove) {
            adaptationModel.getAdaptations().add(adapt(JavaPrimitive.RemoveDeployUnit, currentModel.findByPath(pathDeployUnitToDrop)));
        }
        return adaptationModel;
    }


}
