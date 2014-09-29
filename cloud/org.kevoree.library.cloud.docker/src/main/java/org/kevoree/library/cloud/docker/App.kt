package org.kevoree.library.cloud.docker

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.StringWriter
import org.kevoree.log.Log
import java.nio.file.Files
import java.util.HashMap
import org.kevoree.library.cloud.docker.client.DockerClientImpl
import org.kevoree.library.cloud.docker.model.ContainerConfig
import org.kevoree.library.cloud.docker.model.HostConfig
import org.kevoree.library.cloud.docker.client.DockerException
import org.kevoree.pmodeling.api.json.JSONModelSerializer
import org.kevoree.factory.DefaultKevoreeFactory
import org.kevoree.library.cloud.docker.model.Image
import org.kevoree.library.cloud.docker.model.CommitConfig
import org.kevoree.library.cloud.docker.model.AuthConfig

/**
 * Created by leiko on 20/05/14.
 */
fun main(args: Array<String>) {
    val docker = DockerClientImpl("http://localhost:2375")

    val conf = ContainerConfig()
    conf.setImage("kevoree/js")
    val info = docker.createContainer(conf)!!
    println("Created")
    docker.start(info.getId())
    println("Started")
    val commitConf = CommitConfig()
    commitConf.setContainer(info.getId())
    commitConf.setRepo("maxleiko/foo")
    commitConf.setTag("42")
    docker.commit(commitConf)
    val auth = AuthConfig()
    auth.setEmail("max.tricoire@gmail.com")
    auth.setUsername("maxleiko")
    auth.setPassword("POTATO")
    docker.push("maxleiko/foo", "42", auth)

//    // retrieve current model and serialize it to JSON
//    var serializer = JSONModelSerializer()
//    var factory = DefaultKevoreeFactory()
//    var model = factory.createContainerRoot()
//    var node = factory.createContainerNode()
//    node.name = "myNode" // no TypeDefinition added so this will fail once Kevoree runtime has ended bootstrap
//    // but it's not a problem, the purpose of this file is for docker-java API tests
//    model.addNodes(node)
//    var modelJson = serializer.serialize(model)!!
//
//    try {
//        var container = docker.getContainer(node.name)!!
//        docker.start(container.getId())
//
//    } catch (e: DockerException) {
//        docker.pull(REPO)
//
//        // create and store serialized model in temp dir
//        var dfileFolderPath = Files.createTempDirectory("kevoree_")
//        var dfileFolder : File = File(dfileFolderPath.toString())
//
//        // create temp model
//        var modelFile : File = File(dfileFolder, "boot.json")
//        var writer : BufferedWriter
//        writer = BufferedWriter(FileWriter(modelFile))
//        writer.write(modelJson)
//        writer.close()
//
//        // use newly created image
//        val conf = ContainerConfig()
//        conf.setImage(REPO)
//        var volumes = HashMap<String, Any>();
//        volumes.put(dfileFolder.getAbsolutePath(), HashMap<String, String>());
//        conf.setVolumes(volumes);
//        conf.setCmd(array<String>(
//            "java",
//                "-Dnode.name=${node.name}",
//                "-Dnode.bootstrap=${modelFile.getAbsolutePath()}",
//                "-jar",
//                "/root/kevoree.jar",
//                "release"
//        ))
//
//        var container = docker.createContainer(conf, node.name)!!
//        var hostConf = HostConfig()
//        hostConf.setBinds(array<String>("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:ro"))
//        docker.start(container.getId(), hostConf)
//
//        System.out.println("Container " + container.getId() + " started!")
//    }
}