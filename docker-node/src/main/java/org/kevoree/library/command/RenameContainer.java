package org.kevoree.library.command;

import com.github.dockerjava.api.DockerClient;
import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.library.ModelHelper;
import org.kevoree.modeling.api.KMFContainer;

/**
 *
 * Created by leiko on 9/14/17.
 */
public class RenameContainer implements AdaptationCommand {

    private DockerClient docker;
    private Instance instance;

    public RenameContainer(DockerClient docker, Instance instance) {
        this.docker = docker;
        this.instance = instance;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        String id = instance.findMetaDataByID(ModelHelper.DOCKER_ID).getValue();
        try {
            this.docker.renameContainerCmd(id).withName(instance.getName()).exec();
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to rename container \""+id+"\" with name \""+instance.getName()+"\"", e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new StopContainer(docker, instance).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.UPDATE_PARAM;
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
        return "RenameContainer    " + instance.path();
    }
}
