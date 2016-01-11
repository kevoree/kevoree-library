package org.kevoree.library.docker.killer;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
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
        final List<Container> collect = containers.stream().filter(container -> {
            boolean res;
            try {
                final ContainerInfo containerInfo = dockerClient.inspectContainer(container.id());
                res = containsKeyAndValue(containerInfo.config().env(), key, value);
            } catch (DockerException e) {
                res = false;
            } catch (InterruptedException e) {
                res = false;
            }
            return res;
        }).collect(Collectors.toList());

        final Optional<Container> ret;
        if(collect.isEmpty()) {
            ret = Optional.empty();
        } else {
            ret = Optional.of(collect.get(random.nextInt(collect.size())));
        }
        return ret;
    }

    private <T extends String> boolean containsKeyAndValue(List<T> lst, final T key, final T value) {
        return lst.stream()
                .filter(s -> Objects.equals(s.split("=")[0], key))
                .filter(s -> Objects.equals(s.split("=")[1], value))
                .count() > 0;
    }
}
