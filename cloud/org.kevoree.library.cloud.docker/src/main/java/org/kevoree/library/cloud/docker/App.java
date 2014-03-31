package org.kevoree.library.cloud.docker;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;
import com.kpelykh.docker.client.model.ContainerConfig;
import com.kpelykh.docker.client.model.ContainerCreateResponse;

import java.util.List;

/**
 * Created by duke on 3/28/14.
 */
public class App {

    public static void main(String[] args) throws DockerException {
       System.out.println("Hello");

        DockerClient dockerClient = new DockerClient("http://localhost:4243");

        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImage("busybox");
        containerConfig.setAttachStdout(true);
        containerConfig.setCmd(new String[] {"touch", "/test"});
        ContainerCreateResponse container = dockerClient.createContainer(containerConfig,"hello");



        dockerClient.startContainer(container.getId());

        dockerClient.waitContainer(container.getId());

        dockerClient.stopContainer(container.getId());

        List<Container> containerList = dockerClient.listContainers(true);

        System.out.println(containerList);

    }

}
