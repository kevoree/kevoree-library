package org.kevoree.library.cloud.docker.client;

import org.kevoree.library.cloud.docker.model.*;

import java.util.List;

/***
 * Partial Docker API wrapper
 * Created by leiko on 22/05/14.
 */
public interface DockerClient {

    void start(String id) throws DockerException;
    void start(String id, HostConfig conf) throws DockerException;

    void stop(String id) throws DockerException;

    void deleteContainer(String id) throws DockerException;

    List<Container> getContainers() throws DockerException;

    ContainerDetail getContainer(String idOrName) throws DockerException;

    ImageDetail pull(String name) throws DockerException;
    ImageDetail pull(String name, String tag) throws DockerException;

    ContainerInfo commit(CommitConfig conf) throws DockerException;

    ImageDetail createImage(ImageConfig conf) throws DockerException;

    ContainerInfo createContainer(ContainerConfig conf) throws DockerException;
    ContainerInfo createContainer(ContainerConfig conf, String name) throws DockerException;
}
