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
import com.nirima.docker.client.DockerClient
import com.nirima.docker.client.model.HostConfig
import java.io.StringWriter
import org.apache.commons.io.IOUtils
import com.nirima.docker.client.model.ContainerConfig
import com.nirima.docker.client.model.CommitConfig

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

    private var docker = DockerClient.builder()!!.withUrl("http://localhost:4243")!!.build()!!
    private var containerID: String? = null
    private var hostConf : HostConfig = HostConfig()

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        Log.info("Starting docker container %s ...", containerID)
        docker.containersApi()!!.startContainer(containerID, hostConf)
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        Log.info("Stoping docker container %s ...", containerID)
        docker.containersApi()!!.stopContainer(containerID, 10) // timeToWait in seconds before timeout
        return true
    }

    override fun create() {
        var model : ContainerRoot = modelElement.eContainer() as ContainerRoot

        // pull kevoree/java if not already done
        var res = docker.createPullCommand()!!.image(IMAGE)!!.execute()!!

        var logWriter = StringWriter()
        IOUtils.copy(res, logWriter, "UTF-8")
        Log.debug("$IMAGE pulled")

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

        val container = docker.containersApi()!!.createContainer(modelElement.name, conf)!!
        containerID = container.getId()
        hostConf.setBinds(array<String>("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:ro"))
        docker.containersApi()!!.startContainer(container.getId(), hostConf)

        Log.info("Container "+container.getId()+" started")
    }

    override fun destroy() {
        if (containerID != null) {
            println("DESTROY DockerNodeWrapper")
            val container = docker.container(containerID)!!
            container.createCommitCommand()!!.repo(IMAGE)!!.tag(modelElement.name)!!.execute()
            container.remove(true)
        }
    }
}