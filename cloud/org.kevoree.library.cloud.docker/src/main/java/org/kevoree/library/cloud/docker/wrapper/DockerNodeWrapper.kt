package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import com.kpelykh.docker.client.DockerClient
import com.kpelykh.docker.client.model.ContainerConfig
import com.kpelykh.docker.client.model.CommitConfig

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class DockerNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    val dockerClient: DockerClient = DockerClient("http://localhost:4243")

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        System.out.println("Start docker node ....");
        dockerClient.startContainer(containerID)
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        System.out.println("Stop docker node ....");
        dockerClient.stopContainer(containerID)
        return true
    }

    private var containerID: String? = null;

    override fun create() {
        val containerConfig = ContainerConfig();
        containerConfig.setImage("kevoree/ubuntu");


        containerConfig.setHostName(modelElement.name);
        containerID = dockerClient.createContainer(containerConfig)?.getId()
    }

    override fun destroy() {
        if (containerID != null) {
            println("DESTROY DockerNodeWrapper")
            val conf = CommitConfig(containerID)
            conf.setTag("kevoree/ubuntu")
            dockerClient.commit(conf)
            dockerClient.removeContainer(containerID, true)
        }
    }
}