package org.kevoree.library.docker.killer;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mleduc on 08/01/16.
 */
public class DockerKillerService {

    private final Random random;

    public DockerKillerService() {
        this.random = new Random();
    }

    public DockerKillerService(Random random) {
        this.random = random;
    }

    public Optional<Container> randomlyPickOneMatchingElement(final DefaultDockerClient dockerClient, final String key, final String value) throws DockerException, InterruptedException {
        final List<Container> containers = dockerClient.listContainers();

        final List<Container> collect = new ArrayList<>();
        for (Container container : containers) {
            final ContainerInfo containerInfo = dockerClient.inspectContainer(container.id());
            boolean res;
            res = containsKeyAndValue(containerInfo.config().env(), key, value);
            if (res) {
                collect.add(container);
            }

        }
        final Optional<Container> ret;
        if (collect.isEmpty()) {
            ret = Optional.empty();
        } else {
            ret = Optional.of(collect.get(random.nextInt(collect.size())));
        }
        return ret;
    }

    private <T extends String> boolean containsKeyAndValue(List<T> lst, final T key, final T value) {
        for (T s : lst) {
            final String[] split = s.split("=");
            if (Objects.equals(split[0], key) || Objects.equals(split[1], value)) {
                return true;
            }
        }
        return false;
    }
}
