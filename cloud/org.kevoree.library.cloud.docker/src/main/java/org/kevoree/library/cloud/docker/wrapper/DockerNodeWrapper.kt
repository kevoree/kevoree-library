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
import org.kevoree.Dictionary
import org.kevoree.kcl.api.FlexyClassLoaderFactory
import org.kevoree.library.cloud.docker.DockerNode
import org.kevoree.library.cloud.docker.model.Image
import org.kevoree.TypeDefinition
import org.kevoree.library.cloud.docker.model.ImageConfig
import org.kevoree.library.cloud.docker.model.AuthConfig

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
class DockerNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup,
                        override val bs: BootstrapService, val dockerNode: DockerNode) : KInstanceWrapper {
    override var kcl: ClassLoader? = null

    //    override var kcl : ClassLoader? = null

    override var isStarted: Boolean = false
    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)

    private var docker = DockerClientImpl("http://localhost:2375")
    private var containerID: String? = null
    private var hostConf: HostConfig? = null

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (containerID != null) {
            val id = containerID!!.substring(0, 12)

            // attach container stdout/stderr to System.out/err
            docker.attach(containerID, false, true, false, true, true)

            // start container
            Log.info("Starting docker container {} ...", id)
            docker.start(containerID, hostConf)
            val detail = docker.getContainer(containerID)!!
            val ipAddress = detail.getNetworkSettings()!!.getIpAddress()

            dockerNode.getModelService()!!.submitScript("network ${modelElement.name}.lan.ip $ipAddress", UpdateCallback {});

            Log.info("Docker container $id started at $ipAddress")
        }
        return true
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (containerID != null) {
            val id = containerID!!.substring(0, 12)

            // stop container
            docker.stop(containerID)
            Log.info("Docker container $id successfully stopped")

            // commit container
            var conf = CommitConfig()
            conf.setContainer(containerID)

            var repo = dockerNode.getCommitRepo()
            if (repo == null) {
                throw Exception("DockerNode attribute \"commitRepo\" must be set.")
            }

            var tag = dockerNode.getCommitTag()
            if (tag == null || tag!!.length == 0) {
                tag = "latest";
            }

            conf.setRepo("$repo/${modelElement.name}")
            conf.setTag(tag)
            conf.setMessage(dockerNode.getCommitMsg())
            conf.setAuthor(dockerNode.getCommitAuthor())
            docker.commit(conf)
            Log.info("Container $id commited into ${conf.getRepo()}:$tag")
        }
        return true
    }

    override fun create() {
        val conf : ContainerConfig = ContainerConfig();
        hostConf = HostConfig()

        /**
         * Checks whether or not given "image" is available locally in Docker
         * @param {String} full image tag (repo/name:tag)
         * @return {Boolean} true if image is available locally; false otherwise
         */
        fun isLocallyAvailable(image : String) : Boolean {
            val images : List<Image> = docker.getImages()!!
            for (img in images) {
                for (tag in img.getRepoTags()!!) {
                    if (tag.equals(image)) {
                        return true;
                    }
                }
            }
            return false
        }

        fun configureJavaNode() {
            var model : ContainerRoot = modelElement.eContainer() as ContainerRoot

            // create and store serialized model in temp dir
            var dfileFolder : File = File(Files.createTempDirectory("kevoree_").toString())

            // retrieve current model and serialize it to JSON
            var serializer = JSONModelSerializer()
            var modelJson = serializer.serialize(model)!!

            // create temp model
            var modelFile: File = File(dfileFolder, "boot.json")
            var writer: BufferedWriter
            writer = BufferedWriter(FileWriter(modelFile))
            writer.write(modelJson)
            writer.close()

            var volumes = HashMap<String, Any>();
            volumes.put(dfileFolder.getAbsolutePath(), HashMap<String, String>());
            conf.setVolumes(volumes);
            hostConf!!.setBinds(array("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:rw"))
            conf.setCmd(array(
                    "java",
                    "-Dnode.name=${modelElement.name}",
                    "-Dnode.bootstrap=${modelFile.getAbsolutePath()}",
                    "-jar",
                    "/root/kevboot.jar",
                    "release"
            ))
        }

        fun configureDockerNode() {
            val cpuSharesVal = modelElement.dictionary!!.findValuesByID("cpuShares")
            if (cpuSharesVal != null && cpuSharesVal.value != null && cpuSharesVal.value!!.length > 0) {
                conf.setCpuShares(cpuSharesVal.value!!.toInt())
            }
            val memoryVal = modelElement.dictionary!!.findValuesByID("memory")
            if (memoryVal != null && memoryVal.value != null && memoryVal.value!!.length > 0) {
                conf.setMemoryLimit(memoryVal.value!!.toLong() * 1024 * 1024) // memoryVal.value is in MBytes
            }
            val cmdVal = modelElement.dictionary!!.findValuesByID("cmd")
            if (cmdVal != null && cmdVal.value != null && cmdVal.value!!.length > 0) {
                conf.setCmd(cmdVal.value!!.split(" "))
            }
        }

        var repo = dockerNode.getCommitRepo()
        if (repo == null) {
            throw Exception("DockerNode attribute \"commitRepo\" must be set.")
        }

        var tag = dockerNode.getCommitTag()
        if (tag == null || tag!!.length == 0) {
            tag = "latest";
        }

        val imageName = "$repo/${modelElement.name}:$tag";

        // check if imageName is available locally
        if (isLocallyAvailable(imageName)) {
            // imageName is available locally: use it
            conf.setImage(imageName)

            if (isChildOf(modelElement.typeDefinition!!, "DockerNode")) {
                // child node is a DockerNode
                val dic = modelElement.dictionary
                if (dic != null) {
                    configureDockerNode();
                }

            } else if (isChildOf(modelElement.typeDefinition!!, "JavaNode")) {
                configureJavaNode();
            }

        } else {
            // imageName is not available locally
            // TODO check if image is remotely available
            Log.info("Looking for $repo/${modelElement.name} on remote Docker registry...")
            var searchRes = docker.searchImage("$repo/${modelElement.name}")!!;
            if (searchRes.size() > 0) {
                // image available remotely: pulling it
                docker.pull("$repo/${modelElement.name}")

                conf.setImage(imageName)

                if (isChildOf(modelElement.typeDefinition!!, "DockerNode")) {
                    // child node is a DockerNode
                    val dic = modelElement.dictionary
                    if (dic != null) {
                        configureDockerNode();
                    }

                } else if (isChildOf(modelElement.typeDefinition!!, "JavaNode")) {
                    configureJavaNode();
                }
            } else {
                // imageName is not available remotely
                if (isChildOf(modelElement.typeDefinition!!, "DockerNode")) {
                    // child node is a DockerNode
                    val dic = modelElement.dictionary
                    if (dic != null) {
                        val childImage = dic.findValuesByID("image");
                        if (childImage != null && childImage.value != null && childImage.value!!.length > 0) {
                            conf.setImage(childImage.value)
                            configureDockerNode();
                        } else {
                            throw Exception("DockerNode cannot start a DockerNode if no image is set AND no commitRepo/childNode.name:commitTag image exists")
                        }
                    } else {
                        throw Exception("DockerNode cannot start a DockerNode if no image is set AND no commitRepo/childNode.name:commitTag image exists")
                    }

                } else if (isChildOf(modelElement.typeDefinition!!, "JavaNode")) {
                    // use kevoree/watchdog image when child node is a JavaNode (or one of its subtypes excepted DockerNode)
                    conf.setImage("kevoree/watchdog")
                    configureJavaNode();
                } else {
                    throw Exception("DockerNode does not handle ${modelElement.typeDefinition!!.name} (only DockerNode & JavaNode)")
                }
            }
        }

        // config for every containers
        conf.setAttachStdout(true)
        conf.setAttachStderr(true)

        // create container
        try {
            val contInfos = docker.createContainer(conf, modelElement.name)!!
            containerID = contInfos.getId()
        } catch (e : DockerException) {
            val imgConf = ImageConfig()
            imgConf.setFromImage(conf.getImage()!!.split(":")[0])
            println(imgConf)
            docker.pull(imgConf);
            val contInfos = docker.createContainer(conf, modelElement.name)!!
            containerID = contInfos.getId()
        }
    }

    override fun destroy() {
        if (containerID != null) {
            docker.deleteContainer(containerID)
            Log.info("Container ${containerID!!.substring(0, 12)} successfully deleted")

            // push image
            var repo = dockerNode.getCommitRepo()
            if (repo == null) {
                throw Exception("DockerNode attribute \"commitRepo\" must be set.")
            }

            var tag = dockerNode.getCommitTag()
            if (tag == null || tag!!.length == 0) {
                tag = "latest";
            }

            val pushOnDestroy = dockerNode.getPushOnDestroy()
            if (pushOnDestroy) {
                val auth = AuthConfig()
                auth.setUsername(dockerNode.getAuthUsername())
                auth.setPassword(dockerNode.getAuthPassword()) // FIXME protect password in kevoree model...
                auth.setEmail(dockerNode.getAuthEmail())
                auth.setServerAddress(dockerNode.getPushRegistry())

                docker.push("$repo/${modelElement.name}", tag, auth)
            }
        }
    }

    private fun isChildOf(source : TypeDefinition, target : String) : Boolean {
        if (source.name.equals(target)) {
            return true;
        } else if (source.superTypes.size == 0) {
            return false;
        } else {
            for (parent in source.superTypes) {
                if (parent.name.equals(target)) {
                    return true;
                } else {
                    val isChild = isChildOf(parent, target)
                    if (isChild) {
                        return true;
                    }
                }
            }
            return false
        }
    }
}