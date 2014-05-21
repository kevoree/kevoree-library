package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import org.kevoree.serializer.JSONModelSerializer
import com.kpelykh.docker.client.DockerClient
import com.kpelykh.docker.client.model.ContainerConfig
import com.kpelykh.docker.client.model.CommitConfig
import com.kpelykh.docker.client.model.ContainerCreateResponse
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import org.kevoree.log.Log
import org.kevoree.library.cloud.docker.docker.Dockerfile
import com.sun.jersey.api.client.ClientResponse
import java.nio.file.Files
import org.kevoree.impl.DefaultKevoreeFactory
import com.kpelykh.docker.client.model.HostConfig
import java.util.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class DockerNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    val docker: DockerClient = DockerClient("http://localhost:4243")
    val REPOSITORY : String = "kevoree/java"

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        System.out.println("Start docker node ....")
        docker.startContainer(containerID)
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        System.out.println("Stop docker node ....");
        docker.stopContainer(containerID, 5000)
        return true
    }

    private var containerID: String? = null;

    override fun create() {
        var model : ContainerRoot = modelElement.eContainer() as ContainerRoot

        // pull kevoree/java if not already done
        docker.pull(REPOSITORY)

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
        conf.setImage(REPOSITORY)
        var volumes = HashMap<String, Any>();
        volumes.put(dfileFolder.getAbsolutePath(), HashMap<String, String>());
        conf.setVolumes(volumes);
        conf.setCmd(array<String>(
            "java",
                "-Dnode.name="+modelElement.name,
                "-Dnode.bootstrap="+modelFile.getAbsolutePath(),
                "-jar",
                "/root/kevboot.jar",
                "release"
        ))

        val container = docker.createContainer(conf)!!
        containerID = container.getId()
        var hostConf = HostConfig()
        hostConf.setBinds(array<String>(dfileFolder.getAbsolutePath()+":"+dfileFolder.getAbsolutePath()+":ro"))
        docker.startContainer(container.getId(), hostConf)
        Log.info("Container "+container.getId()+" started")
    }

    override fun destroy() {
        if (containerID != null) {
            println("DESTROY DockerNodeWrapper")
            val conf = CommitConfig(containerID)
            conf.setRepo(REPOSITORY)
            conf.setTag(modelElement.name)
            docker.commit(conf)
            docker.removeContainer(containerID, true)
        }
    }
}