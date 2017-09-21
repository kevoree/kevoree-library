package org.kevoree.library.command;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.PortBinding;
import org.kevoree.Instance;
import org.kevoree.Package;
import org.kevoree.Value;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

import java.util.Arrays;

/**
 *
 * Created by leiko on 9/14/17.
 */
public class UpdateContainer implements AdaptationCommand {

    private DockerClient docker;
    private Instance current;
    private Instance next;
    private State state;

    public UpdateContainer(DockerClient docker, Instance current, Instance next) {
        this.docker = docker;
        this.current = current;
        this.next = next;
        this.state = State.STARTED;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        new StopContainer(docker, current).execute();
        this.state = State.STOPPED;
        new RemoveContainer(docker, current).execute();
        this.state = State.REMOVED;
        new CreateContainer(docker, next).execute();
        this.state = State.CREATED;
        new StartContainer(docker, next).execute();
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        if (this.state == State.STOPPED) {
            // means that StopContainer worked but not the rest
            new StartContainer(docker, current);
        } else if (this.state == State.REMOVED) {
            // means that StopContainer & RemoveContainer worked but not the rest
            new CreateContainer(docker, current).execute();
            new StartContainer(docker, current).execute();
        } else if (this.state == State.CREATED) {
            // means that StopContainer, RemoveContainer & CreateContainer worked but not the rest
            new RemoveContainer(docker, next).execute();
            new CreateContainer(docker, current).execute();
            new StartContainer(docker, current).execute();
        }

    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.UPDATE_INSTANCE;
    }

    @Override
    public KMFContainer getElement() {
        return this.next;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + current.path().hashCode() + next.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "UpdateContainer    " + next.path();
    }

    private enum State {
        STARTED, STOPPED, REMOVED, CREATED
    }
}
