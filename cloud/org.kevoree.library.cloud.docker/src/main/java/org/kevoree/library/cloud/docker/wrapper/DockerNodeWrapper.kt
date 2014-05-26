package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import org.kevoree.serializer.JSONModelSerializer
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import org.kevoree.log.Log
import java.nio.file.Files
import org.kevoree.impl.DefaultKevoreeFactory
import java.util.HashMap
import java.io.StringWriter
import org.kevoree.library.cloud.docker.client.DockerClient
import org.kevoree.library.cloud.docker.model.HostConfig
import org.kevoree.library.cloud.docker.client.DockerClientImpl
import org.kevoree.library.cloud.docker.model.ContainerConfig
import org.kevoree.library.cloud.docker.model.CommitConfig
import org.kevoree.library.cloud.docker.client.DockerException

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
class DockerNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    val IMAGE: String = "kevoree/java"

    private var docker = DockerClientImpl("http://localhost:4243")!!
    private var containerID: String? = null
    private var hostConf : HostConfig = HostConfig()

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        Log.info("Starting docker container {} ...", containerID)
        docker.start(containerID, hostConf)
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        Log.info("Stoping docker container {} ...", containerID)
        docker.stop(containerID)
        return true
    }

    override fun create() {
        var model : ContainerRoot = modelElement.eContainer() as ContainerRoot

        // pull kevoree/java if not already done
        docker.pull(IMAGE)

        // create and store serialized model in temp dir
        var dfileFolderPath = Files.createTempDirectory("docker_")
        var dfileFolder : File = File(dfileFolderPath.toString())

        // retrieve current model and serialize it to JSON
        var serializer = JSONModelSerializer()
        var modelJson = serializer.serialize(model)!!

        // create temp model
        var modelFile : File = File(dfileFolder, "boot.json")
        var writer : BufferedWriter
        writer = BufferedWriter(FileWriter(modelFile))
        writer.write(modelJson)
        writer.close()

        fun createContainer() {
            // create Container configuration
            val conf = ContainerConfig();
            conf.setImage(IMAGE)
            var volumes = HashMap<String, Any>();
            volumes.put(dfileFolder.getAbsolutePath(), HashMap<String, String>());
            conf.setVolumes(volumes);
            conf.setCmd(array<String>(
                    "java",
                    "-Dnode.name=${modelElement.name}",
                    "-Dnode.bootstrap=${modelFile.getAbsolutePath()}",
                    "-jar",
                    "/root/kevboot.jar",
                    "release"
            ))

            val container = docker.createContainer(conf, modelElement.name)!!
            containerID = container.getId()
        }

        fun startContainer() {
            val container = docker.getContainer(modelElement.name)!!
            hostConf.setBinds(array<String>("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:ro"))
            docker.start(container.getId(), hostConf)

            Log.info("Container {} started", container.getId())
        }

        try {
            docker.getContainer(modelElement.name)
            startContainer()

        } catch (e: DockerException) {
            // if getContainer() failed: then we need to create a new container
            createContainer()
            startContainer()
        }
    }

    override fun destroy() {
        if (containerID != null) {
            var conf = CommitConfig()
            conf.setContainer(containerID)
            conf.setRepo(IMAGE)
            conf.setTag(modelElement.name)
            docker.commit(conf)
            docker.deleteContainer(containerID)
        }
    }
}