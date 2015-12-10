package org.kevoree.library.java.planning;

import org.kevoree.*;
import org.kevoree.api.adaptation.AdaptationModel;
import org.kevoree.api.adaptation.AdaptationPrimitive;
import org.kevoree.api.adaptation.AdaptationType;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.trace.*;
import org.kevoree.pmodeling.api.util.ModelVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        return schedule(adaptationModel, nodeName);
    }


    /* Helper to create command */
    private AdaptationPrimitive adapt(AdaptationType primitive, Object elem) {
        AdaptationPrimitive ccmd = new AdaptationPrimitive();
        ccmd.setPrimitiveType(primitive.name());
        ccmd.setRef(elem);
        return ccmd;
    }

    private class TupleObjPrim {
        private KMFContainer obj;

        private TupleObjPrim(KMFContainer obj, AdaptationType p) {
            this.obj = obj;
            this.p = p;
        }

        public AdaptationType getP() {
            return p;
        }

        public void setP(AdaptationType p) {
            this.p = p;
        }

        public KMFContainer getObj() {
            return obj;
        }

        public void setObj(KMFContainer obj) {
            this.obj = obj;
        }

        private AdaptationType p;

        @Override
        public boolean equals(Object second) {
            if (!(second instanceof TupleObjPrim)) {
                return false;
            } else {
                TupleObjPrim secondTuple = (TupleObjPrim) second;
                return secondTuple.getObj().equals(getObj()) && secondTuple.getP().equals(p);
            }
        }

        public String getKey() {
            return getObj().path() + "/" + p.name();
        }

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
        HashMap<String, TupleObjPrim> elementAlreadyProcessed = new HashMap<String, TupleObjPrim>();
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
                
                if (!isVirtual(modelElement)) {
                    if (trace.getRefName().equals("components")) {
                        if (trace.getSrcPath().equals(targetNode.path())) {
                            if (trace instanceof ModelAddTrace) {
                                KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.AddInstance, elemToAdd));
                            }
                            if (trace instanceof ModelRemoveTrace) {
                                KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveInstance, elemToAdd));
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StopInstance, elemToAdd));
                            }
                        }
                    }
                    if (trace.getRefName().equals("hosts")) {
                        if (trace.getSrcPath().equals(targetNode.path())) {
                            if (trace instanceof ModelAddTrace) {
                                KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.AddInstance, elemToAdd));
                            }
                            if (trace instanceof ModelRemoveTrace) {
                                KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveInstance, elemToAdd));
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StopInstance, elemToAdd));
                            }
                        }
                    }
                    if (trace.getRefName().equals("groups")) {
                        if (trace.getSrcPath().equals(targetNode.path())) {
                            if (trace instanceof ModelAddTrace) {
                                KMFContainer elemToAdd = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.AddInstance, elemToAdd));
                            }
                            if (trace instanceof ModelRemoveTrace) {
                                KMFContainer elemToAdd = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveInstance, elemToAdd));
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StopInstance, elemToAdd));
                            }
                        }
                    }
                    if (trace.getRefName().equals("bindings")) {
                        if (!(targetModel.findByPath(trace.getSrcPath()) instanceof Channel)) {
                            if (trace instanceof ModelAddTrace) {
                                MBinding binding = (MBinding) targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                                Channel channel = binding.getHub();
                                if (!isVirtual(channel.getTypeDefinition())) {
                                	adaptationModel.getAdaptations().add(adapt(AdaptationType.AddBinding, binding));
                                    if (channel != null && modelRegistry.lookup(channel) == null) {
                                        TupleObjPrim newTuple = new TupleObjPrim(channel, AdaptationType.AddInstance);
                                        if (!elementAlreadyProcessed.containsKey(newTuple.getKey())) {
                                            adaptationModel.getAdaptations().add(adapt(AdaptationType.AddInstance, channel));
                                            elementAlreadyProcessed.put(newTuple.getKey(), newTuple);
                                        }

                                        if (channel.getDictionary() != null) {
                                            for (Value val : channel.getDictionary().getValues()) {
                                                TupleObjPrim updateVal = new TupleObjPrim(val, AdaptationType.UpdateDictionaryInstance);
                                                if (!elementAlreadyProcessed.containsKey(updateVal)) {
                                                    Object[] values = new Object[] { val.eContainer().eContainer(), val };
                                                    adaptationModel.getAdaptations().add(adapt(AdaptationType.UpdateDictionaryInstance, values));
                                                    elementAlreadyProcessed.put(updateVal.getKey(), updateVal);
                                                }
                                            }

                                        }

                                        for (FragmentDictionary fragDic : channel.getFragmentDictionary()) {
                                            if (fragDic.getName().equals(nodeName)) {
                                                for (Value val : fragDic.getValues()) {
                                                    TupleObjPrim updateVal = new TupleObjPrim(val, AdaptationType.UpdateDictionaryInstance);
                                                    if (!elementAlreadyProcessed.containsKey(updateVal)) {
                                                        Object[] values = new Object[] { val.eContainer().eContainer(), val };
                                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.UpdateDictionaryInstance, values));
                                                        elementAlreadyProcessed.put(updateVal.getKey(), updateVal);
                                                    }
                                                }
                                            }
                                        }

                                        if (channel.getStarted()) {
                                            TupleObjPrim start = new TupleObjPrim(channel, AdaptationType.StartInstance);
                                            if (!elementAlreadyProcessed.containsKey(start.getKey())) {
                                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StartInstance, channel));
                                                elementAlreadyProcessed.put(start.getKey(), start);
                                            }
                                        }
                                    }
                                }
                            }
                            if (trace instanceof ModelRemoveTrace) {
                                org.kevoree.MBinding binding = (org.kevoree.MBinding) targetModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                org.kevoree.MBinding previousBinding = (org.kevoree.MBinding) currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                Channel channel = binding.getHub();
                                Channel oldChannel = previousBinding.getHub();
                                //check if not no current usage of this channel
                                boolean stillUsed = channel != null;
                                if (stillUsed) {
                                    for (MBinding loopBinding : channel.getBindings()) {
                                        if (loopBinding.getPort() != null) {
                                            if (loopBinding.getPort().eContainer().equals(targetNode)) {
                                                stillUsed = true;
                                            }
                                        }
                                    }
                                }
                                if (!stillUsed && modelRegistry.lookup(oldChannel) != null) {
                                    TupleObjPrim removeTuple = new TupleObjPrim(oldChannel, AdaptationType.RemoveInstance);
                                    if (!elementAlreadyProcessed.containsKey(removeTuple.getKey())) {
                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveInstance, oldChannel));
                                        elementAlreadyProcessed.put(removeTuple.getKey(), removeTuple);
                                        TupleObjPrim stopTuple = new TupleObjPrim(oldChannel, AdaptationType.StopInstance);
                                        elementAlreadyProcessed.put(stopTuple.getKey(), stopTuple);
                                    }
                                }
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
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
                                    if (((ModelSetTrace) trace).getContent().toLowerCase().equals("true")) {
                                        TupleObjPrim sIT = new TupleObjPrim(modelElement, AdaptationType.StartInstance);
                                        if (!elementAlreadyProcessed.containsKey(sIT.getKey())) {
                                            adaptationModel.getAdaptations().add(adapt(AdaptationType.StartInstance, modelElement));
                                            elementAlreadyProcessed.put(sIT.getKey(), sIT);
                                        }
                                    } else {
                                        TupleObjPrim sit = new TupleObjPrim(modelElement, AdaptationType.StopInstance);
                                        if (!elementAlreadyProcessed.containsKey(sit.getKey())) {
                                            adaptationModel.getAdaptations().add(adapt(AdaptationType.StopInstance, modelElement));
                                            elementAlreadyProcessed.put(sit.getKey(), sit);
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
                                            TupleObjPrim stopIns = new TupleObjPrim(modelElement, AdaptationType.StopInstance);
                                            if (!elementAlreadyProcessed.containsKey(stopIns.getKey())) {
                                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StopInstance, modelElement));
                                                elementAlreadyProcessed.put(stopIns.getKey(), stopIns);
                                            }
                                        }
                                        //unbind
                                        if (currentModelElement instanceof Channel) {
                                            for (MBinding binding : ((Channel) currentModelElement).getBindings()) {
                                                if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                    adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                }
                                            }
                                        } else {
                                            if (currentModelElement instanceof ComponentInstance) {
                                                for (Port binding : ((ComponentInstance) currentModelElement).getRequired()) {
                                                    if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                    }
                                                }
                                                for (Port binding : ((ComponentInstance) currentModelElement).getProvided()) {
                                                    if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                    }
                                                }
                                            }
                                        }
                                        TupleObjPrim upgradeTuple = new TupleObjPrim(modelElement, AdaptationType.UpgradeInstance);
                                        if (!elementAlreadyProcessed.containsKey(upgradeTuple.getKey())) {
                                            adaptationModel.getAdaptations().add(adapt(AdaptationType.UpgradeInstance, modelElement));
                                            elementAlreadyProcessed.put(upgradeTuple.getKey(), upgradeTuple);
                                        }
                                        //reinject dictionary
                                        List<Value> dicValues = targetModelElement.getDictionary().getValues();
                                        if (dicValues != null) {
                                            for (Value dicValue : dicValues) {
                                                Object[] values = new Object[]{modelElement, dicValue};
                                                adaptationModel.getAdaptations().add(adapt(AdaptationType.UpdateDictionaryInstance, values));
                                                TupleObjPrim updatedicT = new TupleObjPrim(modelElement, AdaptationType.UpdateDictionaryInstance);
                                                elementAlreadyProcessed.put(updatedicT.getKey(), updatedicT);
                                            }
                                        }
                                        //rebind
                                        if (targetModelElement instanceof Channel) {
                                            for (MBinding binding : ((Channel) targetModelElement).getBindings()) {
                                                if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                    adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                }
                                            }
                                        } else {
                                            if (targetModelElement instanceof ComponentInstance) {
                                                for (Port binding : ((ComponentInstance) targetModelElement).getRequired()) {
                                                    if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                    }
                                                }
                                                for (Port binding : ((ComponentInstance) targetModelElement).getProvided()) {
                                                    if (!elementAlreadyProcessed.containsKey(new TupleObjPrim(binding, AdaptationType.RemoveBinding).getKey())) {
                                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveBinding, binding));
                                                    }
                                                }
                                            }
                                        }
                                        //restart
                                        if (currentModelElement.getStarted() == true && targetModelElement.getStarted() == true) {
                                            TupleObjPrim startTuple = new TupleObjPrim(modelElement, AdaptationType.StartInstance);
                                            if (!elementAlreadyProcessed.containsKey(startTuple.getKey())) {
                                                adaptationModel.getAdaptations().add(adapt(AdaptationType.StartInstance, modelElement));
                                                elementAlreadyProcessed.put(startTuple.getKey(), startTuple);
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
                                    TupleObjPrim updateDic = new TupleObjPrim(modelElement, AdaptationType.UpdateDictionaryInstance);
                                    if (!elementAlreadyProcessed.containsKey(updateDic)) {
                                        Object[] values = new Object[]{modelElement.eContainer().eContainer(), modelElement};
                                        adaptationModel.getAdaptations().add(adapt(AdaptationType.UpdateDictionaryInstance, values));
                                        elementAlreadyProcessed.put(updateDic.getKey(), updateDic);
                                    }
                                    if (parentInstance != null) {
                                        TupleObjPrim updateTuple = new TupleObjPrim(parentInstance, AdaptationType.UpdateCallMethod);
                                        if (!elementAlreadyProcessed.containsKey(updateTuple.getKey())) {
                                            adaptationModel.getAdaptations().add(adapt(AdaptationType.UpdateCallMethod, parentInstance));
                                            elementAlreadyProcessed.put(updateTuple.getKey(), updateTuple);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    processTrace(trace, adaptationModel);
                }
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
                        DeployUnit elemDU = (DeployUnit) elem;
                        if (elemDU.findFiltersByID("platform") == null || elemDU.findFiltersByID("platform").getValue().equals("java")) {
                            if (modelRegistry.lookup(elem) == null) {
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.AddDeployUnit, elem));
                                adaptationModel.getAdaptations().add(adapt(AdaptationType.LinkDeployUnit, elem));
                            }
                            foundDeployUnitsToRemove.remove(elem.path());
                        }
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
            adaptationModel.getAdaptations().add(adapt(AdaptationType.RemoveDeployUnit, currentModel.findByPath(pathDeployUnitToDrop)));
        }
        return adaptationModel;
    }

    private boolean isVirtual(KMFContainer element) {
        if (element instanceof Instance) {
            return isVirtual(((Instance) element).getTypeDefinition());
        } else if (element instanceof TypeDefinition) {
            Value virtual = ((TypeDefinition) element).findMetaDataByID("virtual");
            return virtual != null;
        } else if (element instanceof MBinding) {
            MBinding binding = (MBinding) element;
            if (binding.getHub() != null) {
                return isVirtual(binding.getHub().getTypeDefinition());
            }
        }

        return false;
    }

    private boolean isInstallable(TypeDefinition tdef) {
        List<DeployUnit> dus = tdef.getDeployUnits();
        for (DeployUnit du : dus) {
            Value platform = du.findFiltersByID("platform");
            if (platform != null && platform.getValue().equals("java")) {
                return true;
            }
        }
        return false;
    }
}
