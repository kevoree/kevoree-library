package org.kevoree.library.compare;

import com.github.zafarkhaja.semver.Version;
import org.kevoree.*;
import org.kevoree.Dictionary;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.InstanceRegistry;
import org.kevoree.library.command.StartInstance;
import org.kevoree.library.command.StopInstance;
import org.kevoree.library.command.UpdateInstance;
import org.kevoree.library.wrapper.KInstanceWrapper;
import org.kevoree.library.wrapper.WrapperFactory;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.trace.*;
import org.kevoree.modeling.api.util.ModelVisitor;
import org.kevoree.service.ModelService;
import org.kevoree.service.RuntimeService;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Created by duke on 9/26/14.
 */
public class AdaptationEngine {

    private String nodeName;
    private KevoreeFactory adaptationModelFactory = new DefaultKevoreeFactory();
    private ModelCompare modelCompare = adaptationModelFactory.createModelCompare();
    private InstanceRegistry instanceRegistry;
    private CommandFactory cmdFactory;

    public AdaptationEngine(String nodeName, ModelService modelService, RuntimeService runtimeService, InstanceRegistry instanceRegistry,
                            WrapperFactory wrapperFactory) {
        this.nodeName = nodeName;
        this.instanceRegistry = instanceRegistry;
        this.cmdFactory = new CommandFactory(nodeName, runtimeService, modelService, instanceRegistry, wrapperFactory);
    }

    public List<AdaptationCommand> plan(ContainerRoot actualModel, ContainerRoot targetModel) throws KevoreeAdaptationException {
        List<AdaptationCommand> cmds = compareModels(actualModel, targetModel);
        cmds.sort((cmd0, cmd1) -> {
            if (cmd0.getType().getRank() < cmd1.getType().getRank()) {
                return -1;
            } else if (cmd0.getType().getRank() > cmd1.getType().getRank()) {
                return 1;
            } else {
                return 0;
            }
        });
        Map<String, AdaptationCommand> instances2startOrStop = new HashMap<>();
        Map<String, AdaptationCommand> instances2update = new HashMap<>();
        for (AdaptationCommand cmd : cmds) {
            if (cmd instanceof StopInstance || cmd instanceof StartInstance) {
                instances2startOrStop.put(cmd.getElement().path(), cmd);
            }
            if (cmd instanceof UpdateInstance) {
                instances2update.put(cmd.getElement().path(), cmd);
            }
        }
        // if start/stop then no update
        for (Map.Entry<String, AdaptationCommand> entry : instances2startOrStop.entrySet()) {
            AdaptationCommand updateCmd = instances2update.get(entry.getKey());
            if (updateCmd != null) {
                cmds.remove(updateCmd);
            }
        }

        if (Log.TRACE) {
            for (AdaptationCommand cmd : cmds) {
                Log.trace(cmd.toString());
            }
        }
        return cmds;
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

    private void fillAdditional(TraceSequence traces, ContainerNode targetNode, ContainerRoot currentModel, String nodeName) {
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
        // This process can really slow down
        HashSet<String> channelsAlreadySeen = new HashSet<>();
        for (ComponentInstance comp : targetNode.getComponents()) {
            for (Port port : comp.getProvided()) {
                ensureBindings(traces, currentModel, nodeName, channelsAlreadySeen, port);
            }
            for (Port port : comp.getRequired()) {
                ensureBindings(traces, currentModel, nodeName, channelsAlreadySeen, port);
            }
        }
    }

    private void ensureBindings(TraceSequence traces, ContainerRoot currentModel, String nodeName, HashSet<String> channelsAlreadySeen, Port port) {
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

    private List<AdaptationCommand> compareModels(ContainerRoot currentModel, ContainerRoot targetModel)
            throws KevoreeAdaptationException {
        final Set<AdaptationCommand> cmds = new HashSet<>();
        final ContainerNode currentNode = currentModel.findNodesByID(nodeName);
        final ContainerNode targetNode = targetModel.findNodesByID(nodeName);
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
                final KMFContainer modelElement = targetModel.findByPath(trace.getSrcPath());

                if (!isVirtual(modelElement)) {
                    if (trace.getRefName().equals("components")
                            || trace.getRefName().equals("groups")
                            || trace.getRefName().equals("hosts")) {
                        if (trace.getSrcPath().equals(targetNode.path())) {
                            if (trace instanceof ModelAddTrace) {
                                Instance instance = (Instance) targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                                if (!isVirtual(instance)) {
                                    cmds.add(cmdFactory.createAddDeployUnit(validateDeployUnit(instance)));
                                    cmds.add(cmdFactory.createAddInstance(instance));
                                    createDictionaryRelatedCommands(currentModel, instance, cmds);
                                }
                            }
                            if (trace instanceof ModelRemoveTrace) {
                                Instance instance = (Instance) currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                                if (!isVirtual(instance)) {
                                    cmds.add(cmdFactory.createRemoveInstance(instance));
                                    cmds.add(cmdFactory.createStopInstance(instance));
                                }
                            }
                        }
                    }
                    if (trace.getRefName().equals("bindings")) {
                        KMFContainer elem;
                        if (trace instanceof ModelAddTrace) {
                            elem = targetModel.findByPath(((ModelAddTrace) trace).getPreviousPath());
                        } else {
                            elem = currentModel.findByPath(((ModelRemoveTrace) trace).getObjPath());
                        }
                        if (elem != null && elem instanceof MBinding) {
                            MBinding binding = (MBinding) elem;
                            if (isRelatedToPlatform(nodeName, binding)) {
                                if (trace instanceof ModelAddTrace) {
                                    Channel channel = binding.getHub();
                                    if (!isVirtual(channel.getTypeDefinition())) {
                                        cmds.add(cmdFactory.createAddBinding(binding));

                                        if (instanceRegistry.get(channel) == null) {
                                            cmds.add(cmdFactory.createAddDeployUnit(validateDeployUnit(channel)));
                                            cmds.add(cmdFactory.createAddInstance(channel));
                                            createDictionaryRelatedCommands(currentModel, channel, cmds);

                                            if (channel.getStarted()) {
                                                cmds.add(cmdFactory.createStartInstance(channel));
                                            }
                                        }
                                    }
                                } else if (trace instanceof ModelRemoveTrace) {
                                    // check if there will be a usage of this channel
                                    Channel chan = (Channel) targetModel.findByPath(binding.getHub().path());
                                    boolean stillUsed = false;
                                    if (chan != null) {
                                        for (MBinding b: chan.getBindings()) {
                                            if (isRelatedToPlatform(nodeName, b)) {
                                                stillUsed = true;
                                                break;
                                            }
                                        }
                                    }

                                    Channel oldChannel = binding.getHub();
                                    if (!stillUsed && instanceRegistry.get(oldChannel) != null) {
                                        cmds.add(cmdFactory.createRemoveInstance(oldChannel));
                                        if (oldChannel.getStarted()) {
                                            cmds.add(cmdFactory.createStopInstance(oldChannel));
                                        }
                                    }
                                    if (isRelatedToPlatform(nodeName, binding)) {
                                        cmds.add(cmdFactory.createRemoveBinding(binding));
                                        cmds.add(cmdFactory.createUpdateInstance(binding.getHub()));
                                    }
                                }
                            }
                        }
                    }
                    if (trace.getRefName().equals("started")) {
                        if (modelElement instanceof Instance && trace instanceof ModelSetTrace) {
                            Instance instance = (Instance) modelElement;
                            if (isRelatedToPlatform(nodeName, instance)) {
                                if (trace.getSrcPath().equals(targetNode.path())) {
                                    if (((ModelSetTrace) trace).getContent().toLowerCase().equals("false")) {
                                        // do not create a stop command for current node
                                        if (!modelElement.path().equals(currentNode.path())) {
                                            KInstanceWrapper objInstance = (KInstanceWrapper) instanceRegistry.get(modelElement);
                                            if (objInstance != null && objInstance.isStarted()) {
                                                cmds.add(cmdFactory.createStopInstance(instance));
                                            }
                                        }
                                    }
                                } else {
                                    if (((ModelSetTrace) trace).getContent().toLowerCase().equals("true")) {
                                        cmds.add(cmdFactory.createStartInstance(instance));
                                    } else {
                                        KInstanceWrapper objInstance = (KInstanceWrapper) instanceRegistry.get(modelElement);
                                        if (objInstance != null && objInstance.isStarted()) {
                                            cmds.add(cmdFactory.createStopInstance(instance));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (trace.getRefName().equals("typeDefinition")) {
                        if (trace instanceof ModelAddTrace) {
                            if (modelElement instanceof Instance) {
                                Instance currentInstance = (Instance) currentModel.findByPath(modelElement.path());
                                Instance targetInstance = (Instance) targetModel.findByPath(modelElement.path());
                                if (currentInstance != null && targetInstance != null && !isVirtual(currentInstance)) {
                                    // HaraKiri upgrade
                                    if (modelElement.path().equals(targetNode.path())) {
                                        // Serious HaraKiri, should stop the platform and everything .... internalDispatch the core to rebootstrap
                                    } else {
                                        // upgrade internally
                                        if (currentInstance.getStarted() && targetInstance.getStarted()) {
                                            cmds.add(cmdFactory.createStopInstance(currentInstance));
                                        }
                                        // unbind
                                        if (currentInstance instanceof Channel) {
                                            for (MBinding binding : ((Channel) currentInstance).getBindings()) {
                                                cmds.add(cmdFactory.createRemoveBinding(binding));
                                            }
                                        } else if (currentInstance instanceof ComponentInstance) {
                                            for (Port port : ((ComponentInstance) currentInstance).getRequired()) {
                                                for (MBinding binding : port.getBindings()) {
                                                    cmds.add(cmdFactory.createRemoveBinding(binding));
                                                }
                                            }
                                            for (Port port : ((ComponentInstance) currentInstance).getProvided()) {
                                                for (MBinding binding : port.getBindings()) {
                                                    cmds.add(cmdFactory.createRemoveBinding(binding));
                                                }
                                            }
                                        }

                                        cmds.add(cmdFactory.createRemoveInstance(currentInstance));
                                        cmds.add(cmdFactory.createAddDeployUnit(validateDeployUnit(currentInstance)));
                                        cmds.add(cmdFactory.createAddInstance(targetInstance));
                                        createDictionaryRelatedCommands(currentModel, targetInstance, cmds);

                                        // rebind
                                        if (targetInstance instanceof Channel) {
                                            for (MBinding binding : ((Channel) targetInstance).getBindings()) {
                                                cmds.add(cmdFactory.createAddBinding(binding));
                                            }
                                        } else if (targetInstance instanceof ComponentInstance) {
                                            for (Port port : ((ComponentInstance) targetInstance).getRequired()) {
                                                for (MBinding binding : port.getBindings()) {
                                                    cmds.add(cmdFactory.createAddBinding(binding));
                                                }
                                            }
                                            for (Port port : ((ComponentInstance) targetInstance).getProvided()) {
                                                for (MBinding binding : port.getBindings()) {
                                                    cmds.add(cmdFactory.createAddBinding(binding));
                                                }
                                            }
                                        }
                                        // restart
                                        if (currentInstance.getStarted() && targetInstance.getStarted()) {
                                            cmds.add(cmdFactory.createStartInstance(targetInstance));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (trace.getRefName().equals("value")) {
                        if (modelElement instanceof org.kevoree.Value && modelElement.eContainer() instanceof Dictionary) {
                            Value value = (Value) modelElement;
                            Instance instance = (Instance) value.eContainer().eContainer();
                            KMFContainer dictionary = value.eContainer();
                            if (dictionary != null && dictionary instanceof FragmentDictionary && !((FragmentDictionary) dictionary).getName().equals(nodeName)) {
                                // noop
                            } else {
                                if (!isVirtual(instance)) {
                                    cmds.add(cmdFactory.createUpdateParam(instance, value));
                                    Instance previousInstance = (Instance) currentModel.findByPath(instance.path());
                                    if (previousInstance != null && previousInstance.getStarted()) {
                                        cmds.add(cmdFactory.createUpdateInstance(instance));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>(cmds);
    }

    private DeployUnit validateDeployUnit(Instance instance) throws KevoreeAdaptationException {
        List<KMFContainer> metas = instance.getTypeDefinition().select("deployUnits[]/filters[name=platform,value=java]");
        if (metas.size() > 0) {
            if (metas.size() == 1) {
                return (DeployUnit) metas.get(0).eContainer();
            } else {
                return findBestDeployUnit(metas.stream().map(meta -> (DeployUnit) meta.eContainer()).collect(Collectors.toSet()));
                //throw new KevoreeAdaptationException("More than 1 DeployUnit found for " + instance.path() + " and platform=java (must only be one)");
            }
        } else {
            throw new KevoreeAdaptationException("No DeployUnit found for " + instance.path() + " and platform=java");
        }
    }

    private DeployUnit findBestDeployUnit(Set<DeployUnit> dus) {
        Iterator<DeployUnit> it = dus.iterator();
        DeployUnit latest = it.next();
        while (it.hasNext()) {
            DeployUnit du = it.next();
            if (Version.valueOf(latest.getVersion()).lessThan(Version.valueOf(du.getVersion()))) {
                latest = du;
            } else if (Version.valueOf(latest.getVersion()).equals(Version.valueOf(du.getVersion()))) {
                // versions are equals => choose based on timestamp if any
                Value timestamp0 = latest.findFiltersByID("timestamp");
                Value timestamp1 = du.findFiltersByID("timestamp");
                if (timestamp0 != null && timestamp1 != null) {
                    long t0 = Long.valueOf(timestamp0.getValue());
                    long t1 = Long.valueOf(timestamp1.getValue());
                    // if t0 is less than t1 then use the du from t1
                    // if timestamps are equals...highly unlikely but you never know..
                    // well, the first one just won the battle :)
                    if (t0 < t1) {
                        latest = du;
                    }
                } else if (timestamp0 == null && timestamp1 != null) {
                    latest = du;
                }
            }
        }
        return latest;
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
            if (binding.getPort() != null) {
                return isVirtual(binding.getPort().eContainer());
            }
        }

        return false;
    }

    private void createDictionaryRelatedCommands(ContainerRoot currentModel, Instance instance, Set<AdaptationCommand> cmds) {
        createUpdateParamCommands(currentModel, instance.getDictionary(), cmds);

        instance.getFragmentDictionary().forEach(fDic -> {
            if (fDic.getName().equals(nodeName)) {
                createUpdateParamCommands(currentModel, fDic, cmds);
            }
        });
    }

    private void createUpdateParamCommands(ContainerRoot currentModel, Dictionary dictionary, Set<AdaptationCommand> cmds) {
        for (Value value : dictionary.getValues()) {
            Value prevVal = (Value) currentModel.findByPath(value.path());
            if (prevVal != null) {
                if (!prevVal.getValue().equals(value.getValue())) {
                    cmds.add(cmdFactory.createUpdateParam((Instance) dictionary.eContainer(), value));
                }
            } else {
                cmds.add(cmdFactory.createUpdateParam((Instance) dictionary.eContainer(), value));
            }
        }
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

    private boolean isRelatedToPlatform(String nodeName, KMFContainer elem) {
        if (elem instanceof MBinding) {
            MBinding binding = (MBinding) elem;
            if (isRelatedToPlatform(nodeName, binding.getPort())) {
                return true;
            }
            if (binding.getHub() != null) {
                return isRelatedToPlatform(nodeName, binding.getHub());
            }

        } else if (elem instanceof Channel) {
            // if this channel has bindings with components hosted on this node platform: it's ok
            Channel chan = (Channel) elem;
            for (MBinding binding : chan.getBindings()) {
                if (isRelatedToPlatform(nodeName, binding.getPort())) {
                    return true;
                }
            }

        } else if (elem instanceof ComponentInstance) {
            ComponentInstance comp = (ComponentInstance) elem;
            return ((NamedElement) comp.eContainer()).getName().equals(nodeName);

        } else if (elem instanceof Port) {
            if (elem.eContainer() != null) {
                if (isRelatedToPlatform(nodeName, elem.eContainer())) {
                    return true;
                }
            }
        } else if (elem instanceof ContainerNode) {
            ContainerNode node = (ContainerNode) elem;
            return node.getName().equals(nodeName) ||
                    (node.getHost() != null && node.getHost().getName().equals(nodeName));
        } else if (elem instanceof Group) {
            Group group = (Group) elem;
            for (ContainerNode node : group.getSubNodes()) {
                if (isRelatedToPlatform(nodeName, node)) {
                    return true;
                }
            }
        }
        // TODO add every check
        return false;
    }
}
