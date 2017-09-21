package org.kevoree.library;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.Arrays;

/**
 *
 * Created by leiko on 9/19/17.
 */
public class TestDocker {

    public static void main(String[] args) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient client = DockerClientBuilder.getInstance(config).build();

        InspectContainerResponse res = client.inspectContainerCmd("b05c3c310e18").exec();
        System.out.println(String.join(" ", res.getArgs()));
    }
}
