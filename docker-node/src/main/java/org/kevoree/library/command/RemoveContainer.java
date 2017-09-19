package org.kevoree.library.command;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.modeling.api.KMFContainer;

/**
 *
 * Created by leiko on 9/14/17.
 */
public class RemoveContainer implements AdaptationCommand {

    private DockerClient docker;
    private Instance instance;

    public RemoveContainer(DockerClient docker, Instance instance) {
        this.docker = docker;
        this.instance = instance;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        try {
            this.docker.removeContainerCmd(instance.getName()).exec();
        } catch (NotFoundException e) {
            throw new KevoreeAdaptationException("Unable to find container \""+instance.getName()+"\"", e);
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to remove container \""+instance.getName()+"\"", e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new CreateContainer(docker, instance).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.REMOVE_INSTANCE;
    }

    @Override
    public KMFContainer getElement() {
        return this.instance;
    }
}
