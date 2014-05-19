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
        DockerClient dockerClient = new DockerClient("http://localhost:4243");
//        dockerClient.pull("kevoree/ubuntu");

        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImage("kevoree/ubuntu");
        containerConfig.setCmd(new String[] {"/bin/bash"});

        ContainerCreateResponse container = dockerClient.createContainer(containerConfig);
        dockerClient.startContainer(container.getId());
        dockerClient.waitContainer(container.getId());
        System.out.println("Container "+container.getId()+" started!");
//        dockerClient.stopContainer(container.getId());

//        List<Container> containerList = dockerClient.listContainers(true);
//        System.out.println(containerList);

    }

}
