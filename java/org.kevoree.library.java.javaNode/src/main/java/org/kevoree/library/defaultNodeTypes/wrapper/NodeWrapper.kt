package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import java.io.File
import java.util.Arrays
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import org.kevoree.library.defaultNodeTypes.wrapper.NodeWrapper.Reader
import java.io.FileOutputStream
import org.kevoree.api.BootstrapService
import org.kevoree.resolver.MavenResolver
import org.kevoree.serializer.JSONModelSerializer
import org.kevoree.ContainerRoot
import org.kevoree.ContainerNode
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.log.Log

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 17/11/2013
 * Time: 20:03
 */

public class NodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, val nodeName: String, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    class Reader(inputStream: InputStream, val nodeName: String, val error: Boolean) : Runnable{

        val br: BufferedReader

        {
            br = BufferedReader(InputStreamReader(inputStream));
        }

        override fun run() {
            var line: String?;
            try {
                line = br.readLine()
                while(line != null){
                    line = nodeName + "/" + line
                    if(error){
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
        if(!isStarted){
            var platformJar = mavenResolver.resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:" + DefaultKevoreeFactory().getVersion(), Arrays.asList("http://repo1.maven.org/maven2"));
            if(platformJar == null){
                Log.error("Can't download Kevoree platform, abording starting node")
                return false
            }
            Log.info("Fork platform using {}", platformJar!!.getAbsolutePath())
            val tempFile = File.createTempFile("bootModel" + modelElement.name, ".json")
            var tempIO = FileOutputStream(tempFile)
            modelSaver.serializeToStream(tmodel, tempIO)
            tempIO.close()
            tempIO.flush()
            process = Runtime.getRuntime().exec(array(getJava(), "-Dnode.bootstrap=" + tempFile.getAbsolutePath(), "-Dnode.name=" + modelElement.name, "-jar", platformJar!!.getAbsolutePath()))
            readerOUTthread = Thread(Reader(process!!.getInputStream()!!, modelElement.name!!, false))
            readerERRthread = Thread(Reader(process!!.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread!!.start()
            readerERRthread!!.start()
        }
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if(isStarted){
            process?.destroy()
            readerOUTthread?.stop()
            readerERRthread?.stop()
            isStarted = false
        }
        return true
    }

    private fun getJava(): String {
        val java_home: String? = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java"
    }

}