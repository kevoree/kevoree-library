package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import org.kevoree.log.Log
import java.nio.file.Files
import java.util.HashMap
import org.kevoree.library.cloud.docker.model.HostConfig
import org.kevoree.library.cloud.docker.client.DockerClientImpl
import org.kevoree.library.cloud.docker.model.ContainerConfig
import org.kevoree.library.cloud.docker.model.CommitConfig
import org.kevoree.library.cloud.docker.client.DockerException
import org.kevoree.api.ModelService
import org.kevoree.api.handler.UpdateCallback
import org.kevoree.modeling.api.json.JSONModelSerializer

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
class DockerNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup,
                        override val bs: BootstrapService, val modelService: ModelService, val memory: Int,
                        val cpuShares: Int) : KInstanceWrapper {
    override var kcl: ClassLoader? = null
    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    val IMAGE: String = "kevoree/watchdog"

    private var docker = DockerClientImpl("http://localhost:2375")
    private var containerID: String? = null
    private var hostConf: HostConfig = HostConfig()

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (containerID != null) {
            val id = containerID!!.substring(0, 12)
            Log.info("Starting docker container {} ...", id)
            docker.start(containerID, hostConf)
            val detail = docker.getContainer(containerID)!!
            val ipAddress = detail.getNetworkSettings()!!.getIpAddress()

            modelService.submitScript("network ${modelElement.name}.lan.ip $ipAddress", UpdateCallback {
                fun run(b: Boolean) {
                }
            });

            Log.info("Docker container {}{} started at {}", id, detail.getName(), ipAddress)
        }
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (containerID != null) {
            Log.info("Stoping docker container {} ...", containerID)
            docker.stop(containerID)
            Log.info("Docker container {} successfully stopped", containerID)
        }
        return true
    }

    override fun create() {
        var model: ContainerRoot = modelElement.eContainer() as ContainerRoot

        // create and store serialized model in temp dir
        var dfileFolderPath = Files.createTempDirectory("docker_")
        var dfileFolder: File = File(dfileFolderPath.toString())

        // retrieve current model and serialize it to JSON
        var serializer = JSONModelSerializer()
        var modelJson = serializer.serialize(model)!!

        // create temp model
        var modelFile: File = File(dfileFolder, "boot.json")
        var writer: BufferedWriter
        writer = BufferedWriter(FileWriter(modelFile))
        writer.write(modelJson)
        writer.close()

        try {
            var container = docker.getContainer(modelElement.name)!!
            containerID = container.getId()

        } catch (e: DockerException) {
            // if getContainer() failed: then we need to create a new container
            // pull kevoree/java if not already done
            docker.pull(IMAGE)

            // create Container configuration
            val conf = ContainerConfig();
            conf.setImage(IMAGE)
            conf.setMemoryLimit(memory.toLong() * 1024 * 1024) // compute attribute to set limit in MB
            conf.setCpuShares(cpuShares)
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

        } finally {
            hostConf.setBinds(array<String>("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:ro"))
        }
    }

    override fun destroy() {
        if (containerID != null) {
            var conf = CommitConfig()
            conf.setContainer(containerID)
            conf.setRepo(IMAGE)
            conf.setTag(modelElement.name)
            docker.commit(conf)
            Log.info("Container {} commited into {}:{}", containerID, IMAGE, modelElement.name)
            docker.deleteContainer(containerID)
            Log.info("Container {} deleted successfully", containerID, IMAGE, modelElement.name)
        }
    }
}