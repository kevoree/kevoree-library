package org.kevoree.library.cloud.docker.wrapper

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.ContainerRoot
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.ContainerNode
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import org.kevoree.serializer.JSONModelSerializer
import org.kevoree.log.Log
import java.io.File
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.library.cloud.lightlxc.wrapper.ConfigGenerator
import java.util.HashSet
import java.nio.file.Files
import org.kevoree.library.cloud.lightlxc.wrapper.NetworkGenerator
import org.kevoree.library.cloud.lightlxc.wrapper.NodeManager
import org.kevoree.library.cloud.lightlxc.wrapper.MkNodeCommandExecutor
import org.kevoree.library.cloud.lightlxc.wrapper.Reader
import org.kevoree.api.ModelService

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class LightLXCNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService,  val hostitfname: String,
                          val hostitfip: String, val containeripbaseaddress: String,
                           val bridgeName : String,val sshdStart : Boolean, val ip:String,
                           val gw:String,val netmask:String, val mac:String) : KInstanceWrapper {




    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    override var isStarted: Boolean = false
    var process: Process? = null

    var readerOUTthread: Thread? = null
    var readerERRthread: Thread? = null
    private val modelSaver = JSONModelSerializer()


    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        //Log.info("go there 1" + isStarted)
        if (!isStarted) {


            if (process != null) {
                freeze(true)
            } else {
                var factory = DefaultKevoreeFactory( )

                var urls = HashSet<String>()
                urls.add("http://repo1.maven.org/maven2")
                //urls.add("http://localhost/m2/")
                if (factory.getVersion().toLowerCase().contains("snapshot")) {
                    urls.add("http://oss.sonatype.org/content/groups/public/")
                }
                var platformJar = bs.resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:"+ factory.getVersion(), urls);
                if (platformJar == null) {
                    Log.error("Can't download Kevoree platform, aborting starting node "  + modelElement.name!! + " " + factory.getVersion())
                    return false
                }
                Log.debug("Fork platform using {}", platformJar!!.getAbsolutePath())

                var rootUserDirs = Files.createTempDirectory("rootfs").toFile();
                //Log.error("go there")
                Log.info("file" + rootUserDirs?.getAbsolutePath())

                var cg = ConfigGenerator();
                var newUserDir = cg.generateUserDir(rootUserDirs, modelElement, platformJar, bridgeName, ip,netmask,gw, hostitfname,mac,sshdStart)

                val file = File(rootUserDirs.toString() + "/" + modelElement.name!! + "_rootfs/kevrun")
                if (file.exists()) {
                    val bval = file.setExecutable(true);
                }

                val runnerargs = array("lxc-execute", "-n", modelElement.name!!, "-f", rootUserDirs.toString() + "/" + modelElement.name!! + "_rootfs/config", "/kevrun")
                process = Runtime.getRuntime().exec(runnerargs)

                readerOUTthread = Thread(Reader(process!!.getInputStream()!!, modelElement.name!!, false))
                readerERRthread = Thread(Reader(process!!.getErrorStream()!!, modelElement.name!!, true))
                readerOUTthread!!.start()
                readerERRthread!!.start()
            }
        }
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted) {
            freeze(false)
            isStarted = false
        }
        return true
    }

    var freeze = false;

    public fun freeze(un: Boolean) {
        Log.info("freeze call " + un)
        if (freeze == un)
            return
        freeze = un
        var command = "lxc-freeze"
        if (!un) {
            command = "lxc-unfreeze"
        }
        val args = array(command, "-n", modelElement.name!!)
        MkNodeCommandExecutor.execute(modelElement.name!!,args)
    }

    override fun destroy() {


        process?.destroy()

        readerOUTthread?.stop()
        readerERRthread?.stop()
    }
    private fun getJava(): String {
        val java_home: String? = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java"
    }
}