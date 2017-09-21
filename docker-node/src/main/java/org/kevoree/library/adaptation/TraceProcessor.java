package org.kevoree.library.adaptation;

import com.github.dockerjava.api.DockerClient;
import org.kevoree.ContainerRoot;

/**
 *
 * Created by leiko on 9/21/17.
 */
public abstract class TraceProcessor {

    protected DockerClient docker;
    protected String nodeName;
    protected ContainerRoot currentModel;
    protected ContainerRoot targetModel;

    public TraceProcessor(DockerClient docker, String nodeName, ContainerRoot currentModel, ContainerRoot targetModel) {
        this.docker = docker;
        this.nodeName = nodeName;
        this.currentModel = currentModel;
        this.targetModel = targetModel;
    }
}
