package org.kevoree.library.command;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.PortBinding;
import org.kevoree.Instance;
import org.kevoree.Package;
import org.kevoree.Port;
import org.kevoree.Value;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * Created by leiko on 9/14/17.
 */
public class CreateContainer implements AdaptationCommand {

    private DockerClient docker;
    private Instance instance;

    public CreateContainer(DockerClient docker, Instance instance) {
        this.docker = docker;
        this.instance = instance;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        final String repo = ((Package) instance.getTypeDefinition().eContainer()).getName();
        final String image = instance.getTypeDefinition().getName();
        final String tag = instance.getTypeDefinition().getVersion();

        try {
            CreateContainerCmd cmd = this.docker.createContainerCmd(repo + "/" + image + ":" + tag);
            envDecorator(linkDecorator(portDecorator(cmdDecorator(cmd)))).withName(instance.getName()).exec();
        } catch (NotModifiedException ignore) {
            Log.debug("Container \"{}\" ({}) is already created", instance.getName(), repo + "/" + image + ":" + tag);
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to create container \""+instance.getName()+"\"", e);
        }
    }

    private CreateContainerCmd cmdDecorator(CreateContainerCmd createContainerCmd) {
        Value value = instance.getDictionary().findValuesByID("cmd");
        if (value != null) {
            String cmd = value.getValue();
            if (cmd != null && !cmd.isEmpty()) {
                return createContainerCmd.withCmd(cmd.trim().split("\\s"));
            }
        }
        return createContainerCmd;
    }

    private CreateContainerCmd portDecorator(CreateContainerCmd createContainerCmd) {
        Value value = instance.getDictionary().findValuesByID("port");
        if (value != null) {
            String ports = value.getValue();
            if (ports != null && !ports.isEmpty()) {
                PortBinding[] portBindings = Arrays
                        .stream(ports.trim().split("\\s"))
                        .map(PortBinding::parse)
                        .toArray(PortBinding[]::new);
                for (PortBinding binding : portBindings) {
                    System.out.println("binding>>> " + binding.getBinding().toString() + ":" + binding.getExposedPort().toString());
                }
                return createContainerCmd.withPortBindings(portBindings);
            }
        }
        return createContainerCmd;
    }

    private CreateContainerCmd linkDecorator(CreateContainerCmd createContainerCmd) {
        Value value = instance.getDictionary().findValuesByID("link");
        if (value != null) {
            String links = value.getValue();
            if (links != null && !links.isEmpty()) {
                return createContainerCmd
                        .withLinks(Arrays
                                .stream(links.trim().split("\\s"))
                                .map(Link::parse)
                                .toArray(Link[]::new));
            }
        }
        return createContainerCmd;
    }

    private CreateContainerCmd envDecorator(CreateContainerCmd createContainerCmd) {
        Value value = instance.getDictionary().findValuesByID("env");
        if (value != null) {
            String env = value.getValue();
            if (env != null && !env.isEmpty()) {
                return createContainerCmd.withEnv(env.trim().split("\\s"));
            }
        }
        return createContainerCmd;
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new RemoveContainer(docker, instance).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.ADD_INSTANCE;
    }

    @Override
    public KMFContainer getElement() {
        return this.instance;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + instance.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "CreateContainer    " + instance.path();
    }
}
