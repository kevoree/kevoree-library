package org.kevoree.library;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import org.kevoree.*;
import org.kevoree.Dictionary;
import org.kevoree.Package;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.adaptation.AddTraceProcessor;
import org.kevoree.library.adaptation.ControlTraceProcessor;
import org.kevoree.library.adaptation.RemoveTraceProcessor;
import org.kevoree.library.adaptation.SetTraceProcessor;
import org.kevoree.library.command.*;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.ModelCloner;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.trace.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Created by leiko on 9/5/17.
 */
public class ModelHelper {

    public static final String DOCKER_ID = "docker_id";

    private static final String PLATFORM = "docker";
    private static final String DOCKER_REPO = "library";

    private String nodeName;
    private DockerClient docker;
    private KevoreeFactory factory;

    public ModelHelper(String nodeName, DockerClient docker, KevoreeFactory factory) {
        this.nodeName = nodeName;
        this.docker = docker;
        this.factory = factory;
    }

    public ContainerRoot docker2model(final ContainerRoot currentModel) throws KevoreeAdaptationException {
        ModelCloner cloner = this.factory.createModelCloner();
        ContainerRoot dockerModel = cloner.clone(currentModel, false);

        ContainerNode node = dockerModel.findNodesByID(this.nodeName);
        if (node != null) {
            for (ComponentInstance comp : node.getComponents()) {
                if (isDockerRelated(comp)) {
                    comp.delete();
                }
            }
            for (Package pkg : dockerModel.getPackages()) {
                for (TypeDefinition tdef : pkg.getTypeDefinitions()) {
                    if (isDockerRelated(tdef)) {
                        tdef.delete();
                    }
                }
                for (DeployUnit du : pkg.getDeployUnits()) {
                    if (isDockerRelated(du)) {
                        du.delete();
                    }
                }
            }

            this.visitImages(dockerModel);
            this.visitContainers(dockerModel);
        } else {
            throw new KevoreeAdaptationException("Unable to find node \""+nodeName+"\" in currentModel model");
        }

        return dockerModel;
    }

    /**
     * Creates Docker-specific AdaptationCommands to interact with containers.
     * NB. The commands created by this method are Docker-related ONLY, it is not intended to re-create the behavior
     * of the JavaNode AdaptationEngine for instances that are not Docker-related.
     *
     * @param currentModel currentModel in-use model
     * @param targetModel expected model goal after delta
     * @return a list of adaptation commands to execute in order to converge to targetModel (docker-wise only)
     */
    public List<AdaptationCommand> model2docker(final ContainerRoot currentModel, final ContainerRoot targetModel)
            throws KevoreeAdaptationException {
        final Set<AdaptationCommand> cmds = new HashSet<>();
        final AddTraceProcessor addTraceProcessor = new AddTraceProcessor(docker, nodeName, currentModel, targetModel);
        final SetTraceProcessor setTraceProcessor = new SetTraceProcessor(docker, nodeName, currentModel, targetModel);
        final ControlTraceProcessor ctrlTraceProcessor = new ControlTraceProcessor(docker, nodeName, currentModel, targetModel);
        final RemoveTraceProcessor delTraceProcessor = new RemoveTraceProcessor(docker, nodeName, currentModel, targetModel);
        final ModelCompare compare = factory.createModelCompare();


        List<ModelTrace> traces = compare.diff(currentModel, targetModel).getTraces();

        // convert traces to AdaptationCommands
        for (ModelTrace trace : traces) {
            if (trace instanceof ModelAddTrace) {
                cmds.addAll(addTraceProcessor.process((ModelAddTrace) trace));
            } else if (trace instanceof ModelAddAllTrace) {
                cmds.addAll(addTraceProcessor.process((ModelAddAllTrace) trace));
            } else if (trace instanceof ModelSetTrace) {
                cmds.addAll(setTraceProcessor.process((ModelSetTrace) trace));
            } else if (trace instanceof ModelControlTrace) {
                cmds.addAll(ctrlTraceProcessor.process((ModelControlTrace) trace));
            } else if (trace instanceof ModelRemoveTrace) {
                cmds.addAll(delTraceProcessor.process((ModelRemoveTrace) trace));
            } else if (trace instanceof ModelRemoveAllTrace) {
                cmds.addAll(delTraceProcessor.process((ModelRemoveAllTrace) trace));
            }
        }

        // post-process commands
        Map<String, AdaptationCommand> container2remove = new HashMap<>();
        Map<String, AdaptationCommand> container2create = new HashMap<>();
        Map<String, AdaptationCommand> container2start = new HashMap<>();
        Map<String, AdaptationCommand> container2update = new HashMap<>();
        Map<String, AdaptationCommand> container2stop = new HashMap<>();
        for (AdaptationCommand cmd : cmds) {
            Value id = ((Instance) cmd.getElement()).findMetaDataByID(DOCKER_ID);
            if (id != null) {
                if (cmd instanceof RemoveContainer) {
                    container2remove.put(id.getValue(), cmd);
                } else if (cmd instanceof StartContainer) {
                    container2start.put(id.getValue(), cmd);
                } else if (cmd instanceof UpdateContainer) {
                    container2update.put(id.getValue(), cmd);
                } else if (cmd instanceof StopContainer) {
                    container2stop.put(id.getValue(), cmd);
                } else if (cmd instanceof CreateContainer) {
                    container2create.put(id.getValue(), cmd);
                }
            }
        }

        // if container with same id has to be stopped, removed AND created => only rename
        for (Map.Entry<String, AdaptationCommand> entry : container2remove.entrySet()) {
            AdaptationCommand stopCmd = container2stop.get(entry.getKey());
            if (stopCmd != null) {
                final ContainerNode node = targetModel.findNodesByID(nodeName);
                for (ComponentInstance comp : node.getComponents()) {
                    Value id = comp.findMetaDataByID(DOCKER_ID);
                    if (id != null && id.getValue().equals(entry.getKey())) {
                        // valid rename case
                        cmds.remove(entry.getValue()); // remove RemoveContainer cmd
                        cmds.remove(container2stop.get(entry.getKey())); // remove StopContainer cmd
                        cmds.remove(container2create.get(entry.getKey())); // remove CreateContainer cmd
                        cmds.remove(container2start.get(entry.getKey())); // remove StartContainer cmd
                        cmds.add(new RenameContainer(docker, comp)); // add RenameContainer cmd
                    }
                }
            }
        }

        // if container with same id has to start AND update => only start
        for (Map.Entry<String, AdaptationCommand> entry : container2start.entrySet()) {
            AdaptationCommand updateCmd = container2update.get(entry.getKey());
            if (updateCmd != null) {
                cmds.remove(updateCmd);
            }
        }

        // return a sorted list of unique commands
        return sort(cmds);
    }

    private void visitImages(final ContainerRoot model) {
        this.docker
                .listImagesCmd()
                .withShowAll(false)
                .exec()
                .stream()
                .filter(Objects::nonNull)
                .forEach((image) -> this.visit(model, image));
    }

    private void visitContainers(final ContainerRoot model) throws KevoreeAdaptationException {
        List<Container> containers = this.docker.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            this.visit(model, this.docker.inspectContainerCmd(container.getId()).exec());
        }
    }

    private void visit(ContainerRoot model, InspectContainerResponse container) throws KevoreeAdaptationException {
        InspectImageResponse image = this.docker.inspectImageCmd(container.getImageId()).exec();
        ContainerNode node = model.findNodesByID(this.nodeName);
        if (node != null) {
            String containerName = container.getName().substring(1);
            ComponentInstance instance = node.findComponentsByID(containerName);
            if (instance == null) {
                // create component instance
                instance = this.factory.createComponentInstance();
                instance.setName(containerName);
                if (image.getRepoTags() != null) {
                    String repoTag = image.getRepoTags().get(0);
                    String pkgName = getRepo(repoTag);
                    Package pkg = model.findPackagesByID(pkgName);
                    String tdefName = getImageName(repoTag);
                    String tdefVersion = getImageTag(repoTag);
                    if (pkg != null) {
                        TypeDefinition tdef = pkg.findTypeDefinitionsByNameVersion(tdefName, tdefVersion);
                        if (tdef != null) {
                            instance.setTypeDefinition(tdef);
                            node.addComponents(instance);
                        } else {
                            throw new KevoreeAdaptationException("Unable to find the ComponentType for the Docker image \"" + repoTag + "\"");
                        }
                    } else {
                        throw new KevoreeAdaptationException("Unable to find the Package for the Docker image \"" + repoTag + "\"");
                    }
                }
                // set dictionary
                Dictionary dictionary = factory.createDictionary().withGenerated_KMF_ID("0.0");
                instance.setDictionary(dictionary);
                visit(dictionary, container);
            }
            // set running state
            instance.setStarted(container.getState().getRunning());
            // set id
            Value idValue = this.factory.createValue();
            idValue.setName(DOCKER_ID);
            idValue.setValue(container.getId());
            instance.addMetaData(idValue);
        } else {
            throw new KevoreeAdaptationException("Unable to find host node \"" + this.nodeName + "\"");
        }
    }

    private void visit(Dictionary dictionary, InspectContainerResponse container) {
        Value cmdVal = factory.createValue();
        cmdVal.setName("cmd");
        cmdVal.setValue(String.join(" ", container.getArgs()));
        dictionary.addValues(cmdVal);

        Value envVal = factory.createValue();
        envVal.setName("env");
        envVal.setValue(String.join(" ", container.getConfig().getEnv()));
        dictionary.addValues(envVal);

        Value portVal = factory.createValue();
        portVal.setName("port");
        StringBuilder ports = new StringBuilder();
        for (Map.Entry<ExposedPort, Ports.Binding[]> entry: container.getNetworkSettings().getPorts().getBindings().entrySet()) {
            if (entry.getValue() != null) {
                for (Ports.Binding binding: entry.getValue()) {
                    ports.append(binding.toString()).append(":").append(entry.getKey().toString()).append(" ");
                }
            }
        }
        portVal.setValue(ports.toString().trim());
        dictionary.addValues(portVal);

        Value linkVal = factory.createValue();
        linkVal.setName("link");
        StringBuilder links = new StringBuilder();
        for (Link link : container.getHostConfig().getLinks()) {
            links.append(link.getAlias()).append(":").append(link.getName()).append(" ");
        }
        linkVal.setValue(links.toString().trim());
        dictionary.addValues(linkVal);

    }

    private void visit(ContainerRoot model, Image image) {
        if (image.getRepoTags() != null) {
            String repoName = getRepo(image.getRepoTags()[0]);
            Package pkg = model.findPackagesByID(repoName);
            if (pkg == null) {
                pkg = this.factory.createPackage();
                pkg.setName(repoName);
                model.addPackages(pkg);
            }

            ComponentType type = this.factory.createComponentType();
            type.setName(getImageName(image.getRepoTags()[0]));
            type.setVersion(getImageTag(image.getRepoTags()[0]));

            DictionaryType dictionary = factory.createDictionaryType().withGenerated_KMF_ID("0.0");
            DictionaryAttribute cmdAtt = factory.createDictionaryAttribute();
            cmdAtt.setName("cmd");
            cmdAtt.setOptional(true);
            cmdAtt.setDatatype(DataType.STRING);
            dictionary.addAttributes(cmdAtt);
            DictionaryAttribute envAtt = factory.createDictionaryAttribute();
            envAtt.setName("env");
            envAtt.setOptional(true);
            envAtt.setDatatype(DataType.STRING);
            dictionary.addAttributes(envAtt);
            DictionaryAttribute portAtt = factory.createDictionaryAttribute();
            portAtt.setName("port");
            portAtt.setOptional(true);
            portAtt.setDatatype(DataType.STRING);
            dictionary.addAttributes(portAtt);
            DictionaryAttribute linkAtt = factory.createDictionaryAttribute();
            linkAtt.setName("link");
            linkAtt.setOptional(true);
            linkAtt.setDatatype(DataType.STRING);
            dictionary.addAttributes(linkAtt);
            type.setDictionaryType(dictionary);

            Value meta = this.factory.createValue();
            meta.setName("virtual");
            meta.setValue("true");
            type.addMetaData(meta);

            DeployUnit du = this.factory.createDeployUnit();
            du.setName(getImageName(image.getRepoTags()[0]));
            du.setVersion(getImageTag(image.getRepoTags()[0]));
            du.setUrl(image.getId());

            type.addDeployUnits(du);

            Value filter = this.factory.createValue();
            filter.setName("platform");
            filter.setValue(PLATFORM);
            du.addFilters(filter);

            pkg.addDeployUnits(du);
            pkg.addTypeDefinitions(type);
        }
    }

    private String getRepo(String repoTag) {
        String fullRepo = repoTag.split(":")[0];
        String[] splittedRepo = fullRepo.split("/");
        if (splittedRepo.length == 1) {
            return DOCKER_REPO;
        } else {
            return splittedRepo[0];
        }
    }

    private String getImageName(String repoTag) {
        String fullRepo = repoTag.split(":")[0];
        String[] splittedRepo = fullRepo.split("/");
        if (splittedRepo.length == 1) {
            return splittedRepo[0];
        } else {
            return splittedRepo[1];
        }
    }

    private String getImageTag(String repoTag) {
        return repoTag.split(":")[1];
    }

    private List<AdaptationCommand> sort(Set<AdaptationCommand> cmds) {
        List<AdaptationCommand> cmdList = new ArrayList<>(cmds);

        // sort cmds
        cmdList.sort((cmd0, cmd1) -> {
            if (cmd0.getType().getRank() < cmd1.getType().getRank()) {
                return -1;
            } else if (cmd0.getType().getRank() > cmd1.getType().getRank()) {
                return 1;
            } else {
                return 0;
            }
        });

        return cmdList;
    }

    public static boolean isDockerRelated(KMFContainer elem) {
        if (elem instanceof Instance) {
            return isDockerRelated(((Instance) elem).getTypeDefinition());
        } else if (elem instanceof TypeDefinition) {
            if (((TypeDefinition) elem).findMetaDataByID("virtual") != null) {
                return isDockerRelated(((TypeDefinition) elem).getDeployUnits().get(0));
            }
        } else if (elem instanceof DeployUnit) {
            Value platform = ((DeployUnit) elem).findFiltersByID("platform");
            if (platform != null && platform.getValue().equals("docker")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRunning(DockerClient docker, Instance instance) {
        Value id = instance.findMetaDataByID(ModelHelper.DOCKER_ID);
        if (id != null) {
            try {
                InspectContainerResponse containerRes = docker.inspectContainerCmd(id.getValue()).exec();
                return containerRes.getState().getRunning();
            } catch (NotFoundException ignore) {
                return false;
            }
        } else {
            return docker.listContainersCmd().exec()
                    .stream()
                    .filter(container -> container.getNames()[0].equals("/"+instance.getName()))
                    .findFirst()
                    .map(container -> container.getStatus().toLowerCase().startsWith("up"))
                    .orElse(false);
        }
    }

    public static boolean isCreated(DockerClient docker, Instance instance) {
        Value id = instance.findMetaDataByID(ModelHelper.DOCKER_ID);
        if (id != null) {
            try {
                docker.inspectContainerCmd(id.getValue()).exec();
                return true;
            } catch (NotFoundException ignore) {
                return false;
            }
        } else {
            return docker.listContainersCmd().exec()
                    .stream()
                    .anyMatch(container -> container.getNames()[0].equals("/"+instance.getName()));
        }
    }
}
