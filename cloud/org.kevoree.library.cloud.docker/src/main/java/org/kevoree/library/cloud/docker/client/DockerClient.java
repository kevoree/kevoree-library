package org.kevoree.library.cloud.docker.client;

import org.kevoree.library.cloud.docker.model.*;
import us.monoid.json.JSONException;

import java.io.IOException;
import java.util.List;

/***
 * Partial Docker API wrapper
 * Created by leiko on 22/05/14.
 */
public interface DockerClient {

    void start(String id) throws DockerException, JSONException;
    void start(String id, HostConfig conf) throws DockerException, JSONException;

    void stop(String id) throws DockerException;
    void stop(String id, int timeout) throws DockerException;

    void deleteContainer(String id) throws DockerException, JSONException;

    List<Container> getContainers() throws DockerException, JSONException;

    ContainerDetail getContainer(String idOrName) throws DockerException, JSONException;

    void pull(String name) throws DockerException, JSONException;
    void pull(String name, String tag) throws DockerException, JSONException;
    void createImage(ImageConfig conf) throws DockerException, JSONException;

    ContainerInfo commit(CommitConfig conf) throws DockerException, JSONException;

    ContainerInfo createContainer(ContainerConfig conf) throws DockerException, JSONException;
    ContainerInfo createContainer(ContainerConfig conf, String name) throws DockerException, JSONException;
}
