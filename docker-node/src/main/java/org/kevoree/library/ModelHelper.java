package org.kevoree.library;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Link;
import org.kevoree.*;
import org.kevoree.Dictionary;
import org.kevoree.Package;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.command.*;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.ModelCloner;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.trace.ModelAddTrace;
import org.kevoree.modeling.api.trace.ModelRemoveTrace;
import org.kevoree.modeling.api.trace.ModelSetTrace;
import org.kevoree.modeling.api.trace.ModelTrace;

import java.util.*;

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
            throw new KevoreeAdaptationException("Unable to find node \""+nodeName+"\" in current model");
        }

        return dockerModel;
    }

    /**
     * Creates Docker-specific AdaptationCommands to interact with containers.
     * NB. The commands created by this method are Docker-related ONLY, it is not intended to re-create the behavior
     * of the JavaNode AdaptationEngine for instances that are not Docker-related.
     *
     * @param currentModel current in-use model
     * @param targetModel expected model goal after delta
     * @return a list of adaptation commands to execute in order to converge to targetModel (docker-wise only)
     */
    public List<AdaptationCommand> model2docker(final ContainerRoot currentModel, final ContainerRoot targetModel)
            throws KevoreeAdaptationException {
        final List<AdaptationCommand> cmds = new ArrayList<>();

        ModelCompare compare = factory.createModelCompare();
        List<ModelTrace> traces = compare.diff(currentModel, targetModel).getTraces();

        for (ModelTrace trace : traces) {
            if (trace.getRefName().equals("components")) {
                final KMFContainer elem = targetModel.findByPath(trace.getSrcPath());
                if (((NamedElement) elem).getName().equals(nodeName)) {
                    if (trace instanceof ModelAddTrace) {
                        ModelAddTrace addTrace = (ModelAddTrace) trace;
                        Instance instance = (Instance) targetModel.findByPath(addTrace.getPreviousPath());
                        if (isDockerRelated(instance)) {
                            Value id = instance.findMetaDataByID(DOCKER_ID);
                            if (id != null) {
                                try {
                                    docker.inspectContainerCmd(id.getValue()).exec();
                                } catch (NotFoundException ignore) {
                                    cmds.add(new CreateContainer(docker, instance));
                                    cmds.add(new StartContainer(docker, instance));
                                }
                            } else {
                                docker.listContainersCmd()
                                        .exec()
                                        .stream()
                                        .filter(container -> container.getNames()[0].equals(instance.getName()))
                                        .findFirst()
                                        .orElseGet(() -> {
                                            cmds.add(new CreateContainer(docker, instance));
                                            cmds.add(new StartContainer(docker, instance));
                                            return null;
                                        });
                            }
                        }
                    } else if (trace instanceof ModelRemoveTrace) {
                        ModelRemoveTrace removeTrace = (ModelRemoveTrace) trace;
                        Instance instance = (Instance) currentModel.findByPath(removeTrace.getObjPath());
                        if (isDockerRelated(instance)) {
                            Value id = instance.findMetaDataByID(DOCKER_ID);
                            if (id != null) {
                                try {
                                    docker.inspectContainerCmd(id.getValue()).exec();
                                    cmds.add(new StopContainer(docker, instance));
                                    cmds.add(new RemoveContainer(docker, instance));
                                } catch (NotFoundException ignore) {}
                            } else {
                                docker.listContainersCmd()
                                        .exec()
                                        .stream()
                                        .filter(container -> container.getNames()[0].equals(instance.getName()))
                                        .findFirst()
                                        .ifPresent(container -> {
                                            cmds.add(new StopContainer(docker, instance));
                                            cmds.add(new RemoveContainer(docker, instance));
                                        });
                            }
                        }
                    }
                }
            } else if (trace.getRefName().equals("started")) {
                if (trace instanceof ModelSetTrace) {
                    ModelSetTrace setTrace = (ModelSetTrace) trace;
                    final KMFContainer elem = targetModel.findByPath(trace.getSrcPath());
                    if (elem instanceof ComponentInstance) {
                        ComponentInstance instance = (ComponentInstance) elem;
                        if (((ContainerNode) instance.eContainer()).getName().equals(nodeName)) {
                            if (setTrace.getContent().toLowerCase().equals("false")) {
                                // stop behavior
                                ContainerNode node = targetModel.findNodesByID(nodeName);
                                if (node.getStarted()) {
                                    // only stop containers if the node platform is not stopping
                                    Value id = instance.findMetaDataByID(DOCKER_ID);
                                    if (id != null) {
                                        try {
                                            InspectContainerResponse containerRes = docker.inspectContainerCmd(id.getValue()).exec();
                                            if (containerRes.getState().getRunning()) {
                                                cmds.add(new StopContainer(docker, instance));
                                            }
                                        } catch (NotFoundException ignore) {}
                                    } else {
                                        docker.listContainersCmd().exec().stream().filter(
                                                container -> container.getNames()[0].equals(instance.getName())
                                        ).findFirst().ifPresent(container -> {
                                            if (container.getStatus().toLowerCase().startsWith("up")) {
                                                cmds.add(new StopContainer(docker, instance));
                                            }
                                        });
                                    }
                                }
                            } else if (setTrace.getContent().equals("true")) {
                                Value id = instance.findMetaDataByID(DOCKER_ID);
                                if (id != null) {
                                    try {
                                        InspectContainerResponse containerRes = docker.inspectContainerCmd(id.getValue()).exec();
                                        if (!containerRes.getState().getRunning()) {
                                            cmds.add(new StartContainer(docker, instance));
                                        }
                                    } catch (NotFoundException ignore) {}
                                } else {
                                    docker.listContainersCmd().exec().stream().filter(
                                            container -> container.getNames()[0].equals(instance.getName())
                                    ).findFirst().ifPresent(container -> {
                                        if (container.getStatus().toLowerCase().startsWith("exited")) {
                                            cmds.add(new StartContainer(docker, instance));
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }
        }

        // post-process commands
        Map<String, AdaptationCommand> container2remove = new HashMap<>();
        Map<String, AdaptationCommand> container2stop = new HashMap<>();
        for (AdaptationCommand cmd : cmds) {
            Value id = ((Instance) cmd.getElement()).findMetaDataByID(DOCKER_ID);
            if (id != null) {
                if (cmd instanceof RemoveContainer) {
                    container2remove.put(id.getValue(), cmd);
                } else if (cmd instanceof StopContainer) {
                    container2stop.put(id.getValue(), cmd);
                }
            }
        }
        // if containers with same id has to be removed AND stop AND still in target model => only rename
        for (Map.Entry<String, AdaptationCommand> entry : container2remove.entrySet()) {
            AdaptationCommand stopCmd = container2stop.get(entry.getKey());
            if (stopCmd != null) {
                final ContainerNode node = targetModel.findNodesByID(nodeName);
                for (ComponentInstance comp : node.getComponents()) {
                    Value id = comp.findMetaDataByID(DOCKER_ID);
                    if (id != null && id.getValue().equals(entry.getKey())) {
                        // rename case
                        cmds.remove(entry.getValue());
                        cmds.remove(stopCmd);
                        cmds.add(new RenameContainer(docker, comp));
                    }
                }
            }
        }

        // sort cmds
        cmds.sort((cmd0, cmd1) -> {
            if (cmd0.getType().getRank() < cmd1.getType().getRank()) {
                return -1;
            } else if (cmd0.getType().getRank() > cmd1.getType().getRank()) {
                return 1;
            } else {
                return 0;
            }
        });

        return cmds;
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
            }
            // set dictionary
            Dictionary dictionary = factory.createDictionary().withGenerated_KMF_ID("0.0");
            instance.setDictionary(dictionary);
            visit(dictionary, container);
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
        portVal.setValue(""); // TODO
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

    private boolean isDockerRelated(KMFContainer elem) {
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

    private String getImageTag(String repoTag) {
        return repoTag.split(":")[1];
    }
}
