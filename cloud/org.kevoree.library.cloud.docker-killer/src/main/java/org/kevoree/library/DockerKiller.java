package org.kevoree.library;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import org.kevoree.annotation.*;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.util.Optional;

/**
 * Created by mleduc on 11/01/16.
 */
@ComponentType(version = 1, description = "Randomly kill a container when a input is done. A container can be kill only is one of its environment " +
        "variable match the key and value defined in the dictionary.")
public class DockerKiller {

    private final DockerKillerService dockerKiller = new DockerKillerService();
    private DefaultDockerClient dockerClient;

    @Param(optional = false)
    private String key;

    @Param(optional = false)
    private String value;

    @Param(defaultValue = "5", optional = false)
    private Integer secondsToWaitBeforeKilling;

    @Output
    public Port killedContainer;

    @Start
    public void start() {
        try {
            this.dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            Log.error(e.getMessage());
            this.dockerClient = null;
        }
    }

    @Input
    public void trigger() {
        if (dockerClient != null) {
            try {
                final Optional<Container> container = dockerKiller.randomlyPickOneMatchingElement(dockerClient, key, value);
                if(container.isPresent()) {
                    final String containerId = container.get().id();
                    dockerClient.stopContainer(containerId, secondsToWaitBeforeKilling);
                    killedContainer.send(containerId);
                }
            } catch (DockerException e) {
                Log.error(e.getMessage());
            } catch (InterruptedException e) {
                Log.error(e.getMessage());
            }
        } else {
            Log.info("No action because of null docker client");
        }
    }
}
