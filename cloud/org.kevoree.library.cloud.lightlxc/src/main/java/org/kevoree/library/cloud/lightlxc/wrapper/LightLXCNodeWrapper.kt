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
import org.kevoree.resolver.MavenResolver
import org.kevoree.serializer.JSONModelSerializer
import org.kevoree.log.Log
import java.io.File
import java.io.FileOutputStream
import org.kevoree.impl.DefaultKevoreeFactory
import java.util.Arrays
import org.kevoree.library.cloud.lightlxc.wrapper.ConfigGenerator
import java.util.HashSet
import java.nio.file.Files

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class LightLXCNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {


    class Reader(inputStream: InputStream, val nodeName: String, val error: Boolean) : Runnable{

        val br: BufferedReader

        {
            br = BufferedReader(InputStreamReader(inputStream));
        }

        override fun run() {
            var line: String?;
            try {
                line = br.readLine()
                while (line != null) {
                    line = nodeName + "/" + line
                    if (error) {
                        System.err.println(line);
                    } else {
                        System.out.println(line);
                    }
                    line = br.readLine()
                }
            } catch (e: IOException) {
                e.printStackTrace();
            }
        }
    }

    override val resolver: MethodAnnotationResolver = MethodAnnotationResolver(targetObj.javaClass)
    override var isStarted: Boolean = false
    val mavenResolver = MavenResolver()
    var process: Process? = null

    var readerOUTthread: Thread? = null
    var readerERRthread: Thread? = null
    private val modelSaver = JSONModelSerializer()



    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        Log.info("go there 1")
        /*if (!isStarted) {

            if (process != null) {
                freeze(true)
            } else {

                var urls = HashSet<String>()
                urls.add("http://repo1.maven.org/maven2")
                var platformJar = mavenResolver.resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:" + DefaultKevoreeFactory().getVersion(), urls);
                if (platformJar == null) {
                    Log.error("Can't download Kevoree platform, abording starting node")
                    return false
                }
                Log.info("Fork platform using {}", platformJar!!.getAbsolutePath())


                var rootUserDirs =  Files.createTempDirectory("rootfs").toFile();
                Log.error("go there")
                Log.error("file" + rootUserDirs?.getAbsolutePath())
                var cg = ConfigGenerator();
                var newUserDir = cg.generateUserDir(rootUserDirs, modelElement, platformJar);
                val runnerargs = array("lxc-execute", "-n", modelElement.name!!, "-f", File(newUserDir, "config").getAbsolutePath(), "/kevrun")
                process = Runtime.getRuntime().exec(runnerargs)
                readerOUTthread = Thread(Reader(process!!.getInputStream()!!, modelElement.name!!, false))
                readerERRthread = Thread(Reader(process!!.getErrorStream()!!, modelElement.name!!, true))
                readerOUTthread!!.start()
                readerERRthread!!.start()
            }
        }*/
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted) {
            freeze(false)
            isStarted = false
        }
        return true
    }

    private fun freeze(un: Boolean) {
        var command = "lxc-freeze"
        if (un) {
            command = "lxc-unfreeze"
        }
        val args = array(command, "-n", modelElement.name!!)
        Runtime.getRuntime().exec(args).waitFor()
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