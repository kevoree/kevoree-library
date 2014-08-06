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
import org.kevoree.ContainerRoot
import org.kevoree.ContainerNode
import org.kevoree.log.Log
import java.util.HashSet
import org.kevoree.modeling.api.json.JSONModelSerializer
import org.kevoree.factory.DefaultKevoreeFactory

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
    var process: Process? = null

    var readerOUTthread: Thread? = null
    var readerERRthread: Thread? = null
    private val modelSaver = JSONModelSerializer()

    private var tempFile: File? = null

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!isStarted) {
            var urls = HashSet<String>()
            urls.add("http://repo1.maven.org/maven2")
            val version = DefaultKevoreeFactory().getVersion()
            if (version.toString().contains("SNAPSHOT")) {
                urls.add("http://oss.sonatype.org/content/groups/public/")
            }
            var platformJar = bs.resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:" + version, urls);
            if (platformJar == null) {
                Log.error("Can't download Kevoree platform, abording starting node")
                return false
            }
            var jvmArgs: String? = null
            if (modelElement.dictionary != null) {
                var jvmArgsAttribute = modelElement.dictionary!!.findValuesByID("jvmArgs")
                if (jvmArgsAttribute != null) {
                    jvmArgs = jvmArgsAttribute.toString()
                }
            }
            Log.debug("Fork platform using {}", platformJar!!.getAbsolutePath())
            tempFile = File.createTempFile("bootModel" + modelElement.name, ".json")
            var tempIO = FileOutputStream(tempFile!!)
            modelSaver.serializeToStream(tmodel, tempIO)
            tempIO.close()
            tempIO.flush()

            var classPath = System.getProperty("java.class.path");
            var newClassPath = StringBuilder()
            var classPathList = classPath?.split(':')
            newClassPath.append(platformJar!!.getAbsolutePath())
            classPathList?.forEach { cpe ->
               if(!cpe.contains("org.kevoree.platform.standalone-")){
                   newClassPath.append(File.pathSeparator)
                   newClassPath.append(cpe)
               }
            }

            var devOption = "-Dkevoree.prod=true";
            if(System.getProperty("kevoree.dev")!=null){
                devOption = "-Dkevoree.dev="+System.getProperty("kevoree.dev")!!
            }

            var execArray = array(getJava(),"-cp",newClassPath.toString(),devOption,"-Dnode.bootstrap=" + tempFile!!.getAbsolutePath(), "-Dnode.name=" + modelElement.name,"org.kevoree.platform.standalone.App")
            if (jvmArgs != null) {
                execArray = array(getJava(), jvmArgs!!,"-cp",newClassPath.toString(),devOption,"-Dnode.bootstrap=" + tempFile!!.getAbsolutePath(), "-Dnode.name=" + modelElement.name,"org.kevoree.platform.standalone.App")
            }

            process = Runtime.getRuntime().exec(execArray)
            readerOUTthread = Thread(Reader(process!!.getInputStream()!!, modelElement.name!!, false))
            readerERRthread = Thread(Reader(process!!.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread!!.start()
            readerERRthread!!.start()
            isStarted = true
        }
        return true
    }
    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted) {
            process?.destroy()
            process?.waitFor()
            readerOUTthread?.stop()
            readerERRthread?.stop()
            tempFile?.delete()
            isStarted = false
        }
        return true
    }

    private fun getJava(): String {
        val java_home: String? = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java"
    }

}