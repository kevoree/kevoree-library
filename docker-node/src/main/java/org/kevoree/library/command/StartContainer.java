package org.kevoree.library.command;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotModifiedException;
import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

/**
 *
 * Created by leiko on 9/14/17.
 */
public class StartContainer implements AdaptationCommand {

    private DockerClient docker;
    private Instance instance;

    public StartContainer(DockerClient docker, Instance instance) {
        this.docker = docker;
        this.instance = instance;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        try {
            this.docker.startContainerCmd(instance.getName()).exec();
        } catch (NotModifiedException ignore) {
            Log.debug("Container \"{}\" is already started", instance.getName());
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to start container \""+instance.getName()+"\"", e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new StopContainer(docker, instance).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.START_INSTANCE;
    }

    @Override
    public KMFContainer getElement() {
        return this.instance;
    }
}
