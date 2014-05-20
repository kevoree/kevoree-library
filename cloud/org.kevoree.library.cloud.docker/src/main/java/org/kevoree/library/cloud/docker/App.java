package org.kevoree.library.cloud.docker;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;
import com.kpelykh.docker.client.model.ContainerConfig;
import com.kpelykh.docker.client.model.ContainerCreateResponse;
import org.kevoree.library.cloud.docker.docker.Dockerfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by duke on 3/28/14.
 */
//public class App {
//
//    public static void main(String[] args) throws DockerException {
//        DockerClient docker = new DockerClient("http://localhost:4243");
//
////        docker.pull("kevoree/java");
////
////        ContainerConfig conf = new ContainerConfig();
////        conf.setImage("kevoree/java");
////        conf.setCmd(new String[]{"/sbin/my_init", "--", "java", "-Dnode.name=potato", "-jar", "/root/kevoree.jar"});
////
////        ContainerCreateResponse container = docker.createContainer(conf);
////        docker.startContainer(container.getId());
////        System.out.println("Container "+container.getId()+" started!");
////
////        List<Container> containerList = docker.listContainers(true);
////        System.out.println(containerList);
//    }
//}
