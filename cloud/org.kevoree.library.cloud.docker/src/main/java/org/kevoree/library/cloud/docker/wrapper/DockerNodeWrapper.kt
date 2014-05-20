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

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        System.out.println("Start docker node ....")
        docker.startContainer(containerID)
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        System.out.println("Stop docker node ....");
        docker.stopContainer(containerID)
        return true
    }

    private var containerID: String? = null;

    override fun create() {
        var model : ContainerRoot = modelElement.eContainer() as ContainerRoot

        // create Dockerfile
        var dockerfile : Dockerfile = Dockerfile(model, "password")

        // build Docker image using Dockerfile
        docker.build(dockerfile.getFile(), "kevoree/java:"+model.generated_KMF_ID)

        // pull kevoree/java from Docker repository
        docker.pull("kevoree/java:"+model.generated_KMF_ID)

        // create Container configuration
        val conf = ContainerConfig();
        conf.setImage("kevoree/java")
        conf.setCmd(array<String>(
                "/sbin/my_init", "--",
                "java",
                    "-Dnode.name="+modelElement.name,
                    "-Dnode.bootstrap=/root/boot.json",
                    "-jar",
                    "/root/kevoree.jar"
        ))

        val container = docker.createContainer(conf)!!
        containerID = container.getId()
        docker.startContainer(container.getId())
        Log.info("Container "+container.getId()+" started")
    }

    override fun destroy() {
        if (containerID != null) {
            println("DESTROY DockerNodeWrapper")
            val conf = CommitConfig(containerID)
            var model : ContainerRoot = modelElement.eContainer() as ContainerRoot
            conf.setTag("kevoree/java:"+model.generated_KMF_ID)
            docker.commit(conf)
            docker.removeContainer(containerID, true)
        }
    }
}