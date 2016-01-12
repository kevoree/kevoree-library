package org.kevoree.library;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import org.jetbrains.annotations.NotNull;
import org.kevoree.annotation.*;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.io.IOException;
import java.util.*;

/**
 * Created by leiko on 16/10/15.
 */
@ComponentType(description = "Send a signal where the number of docker containers match the filter fall under a defined limit.")
public class DockerMonitor {

    private DockerClient docker;


    @Output
    private Port tickOut;

    @Param(optional = false)
    private String key;

    @Param(optional = false)
    private String value;

    @Param(optional = false, defaultValue = "10")
    private Integer limit;

    @Start
    public void start() throws DockerCertificateException, DockerException, InterruptedException, IOException {
        this.docker = DefaultDockerClient.fromEnv().build();
    }

    @Input
    public void tickIn() throws DockerException, InterruptedException {
        if (docker != null) {
            final List<Container> containers = docker.listContainers();
            final List<Container> matching = search(containers);

            if (matching.size() < limit) {
                tickOut.send("{\"too_low\": true}");
            }
        } else {
            Log.info("No action because of null docker client");
        }
    }

    @NotNull
    private List<Container> search(List<Container> containers) throws DockerException, InterruptedException {
        final List<Container> matching = new ArrayList<>();
        for (Container container : containers) {
            final ContainerInfo containerInfo = docker.inspectContainer(container.id());
            final List<String> env = containerInfo.config().env();
            boolean found = filter(env);

            if (found) {
                matching.add(container);
            }
        }
        return matching;
    }

    private boolean filter(List<String> env) {
        boolean found = false;
        for (String e : env) {
            if (Objects.equals(key, e.split("=")[0]) || Objects.equals(value, e.split("=")[1])) {
                found = true;
                break;
            }
        }
        return found;
    }
}
