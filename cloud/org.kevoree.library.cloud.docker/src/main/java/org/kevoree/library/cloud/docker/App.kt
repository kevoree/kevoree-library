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
import java.util.HashMap
import com.kpelykh.docker.client.model.HostConfig

/**
 * Created by leiko on 20/05/14.
 */
fun main(args: Array<String>) {
    val REPO = "kevoree/java"
    var docker = DockerClient("http://localhost:4243");
    docker.pull(REPO)

    // create and store serialized model in temp dir
    var dfileFolderPath = Files.createTempDirectory("docker_")
    var dfileFolder : File = File(dfileFolderPath.toString())

    // retrieve current model and serialize it to JSON
    var serializer = JSONModelSerializer()
    var factory = DefaultKevoreeFactory()
    var model = factory.createContainerRoot()
    var node = factory.createContainerNode()
    node.name = "myNode" // no TypeDefinition added so this will fail once Kevoree runtime has ended bootstrap
                         // but it's not a problem, the purpose of this file is for docker-java API tests
    model.addNodes(node)
    var modelJson = serializer.serialize(model)!!

    // create temp model
    var modelFile : File = File(dfileFolder, "boot.json")
    var writer : BufferedWriter
    writer = BufferedWriter(FileWriter(modelFile))
    writer.write(modelJson)
    writer.close()

    // use newly created image
    val conf = ContainerConfig()
    conf.setImage(REPO)
    var volumesMap = HashMap<String, Object>();
    volumesMap.put(dfileFolder.getAbsolutePath(), HashMap<String, String>());
    conf.setVolumes(volumesMap);
    conf.setCmd(array<String>(
        "java",
            "-Dnode.name="+node.name,
            "-Dnode.bootstrap="+modelFile.getAbsolutePath(),
            "-jar",
            "/root/kevboot.jar",
            "release"
    ))

    val container = docker.createContainer(conf)!!

    var hostConf = HostConfig()
    hostConf.setBinds(array<String>(dfileFolder.getAbsolutePath()+":"+dfileFolder.getAbsolutePath()+":ro"))
    docker.startContainer(container.getId(), hostConf)

    System.out.println("Container " + container.getId() + " started!")
}