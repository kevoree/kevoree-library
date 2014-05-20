package org.kevoree.library.cloud.docker

import com.kpelykh.docker.client.DockerClient
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import org.kevoree.library.cloud.docker.docker.Dockerfile
import java.io.StringWriter
import org.apache.commons.io.IOUtils
import org.kevoree.log.Log
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.serializer.JSONModelSerializer
import java.nio.file.Files
import com.kpelykh.docker.client.model.ContainerConfig
import com.kpelykh.docker.client.model.ContainerCreateResponse
import com.kpelykh.docker.client.model.Container

/**
 * Created by leiko on 20/05/14.
 */
fun main(args: Array<String>) {
    val TAG = "kevoree/java:17586642171400581861086"
    var docker = DockerClient("http://localhost:4243");

    // empty model (to test boot.json creation)
    var factory = DefaultKevoreeFactory()
    var model = factory.createContainerRoot()

    var dockerfile : Dockerfile = Dockerfile(model, "password")

    // build Docker image using Dockerfile
    var res = docker.build(dockerfile.getFile(), TAG)

    // use newly created image
    val conf = ContainerConfig()
    conf.setImage(TAG)
    conf.setCmd(array<String>(
            "/sbin/my_init", "--",
            "java",
                "-Dnode.name=myNode",
                "-Dnode.bootstrap=/root/boot.json",
                "-jar",
                "/root/kevoree.jar"
    ))

    val container = docker.createContainer(conf)!!
    docker.startContainer(container.getId())
    System.out.println("Container " + container.getId() + " started!")
}