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
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.library.cloud.lightlxc.wrapper.ConfigGenerator
import java.util.HashSet
import java.nio.file.Files
import org.kevoree.library.cloud.lightlxc.wrapper.NetworkGenerator
import org.kevoree.library.cloud.lightlxc.wrapper.NodeManager

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class LightLXCNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService, val routeditfname: String, val hostitfname: String, val hostitfip: String, val containeripbaseaddress: String, val createBrdge: Boolean, val bridgeName : String) : KInstanceWrapper {


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
        //Log.info("go there 1" + isStarted)
        if (!isStarted) {

            if (process != null) {
                freeze(true)
            } else {
                var factory = DefaultKevoreeFactory()
                var urls = HashSet<String>()
                urls.add("http://repo1.maven.org/maven2")
                if (factory.getVersion().toLowerCase().contains("snapshot")) {
                    urls.add("http://oss.sonatype.org/content/groups/public/")
                }
                var platformJar = mavenResolver.resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:" + factory.getVersion(), urls);
                if (platformJar == null) {
                    Log.error("Can't download Kevoree platform, abording starting node")
                    return false
                }
                Log.info("Fork platform using {}", platformJar!!.getAbsolutePath())


                var rootUserDirs = Files.createTempDirectory("rootfs").toFile();
                //Log.error("go there")
                Log.info("file" + rootUserDirs?.getAbsolutePath())
                val ng = NetworkGenerator(this.containeripbaseaddress, this.hostitfip)

                var cg = ConfigGenerator();

                val ethname = this.hostitfname
                var newUserDir = cg.generateUserDir(rootUserDirs, modelElement, platformJar, bridgeName, ng, routeditfname)

                if (createBrdge) {
                    // TODO check and fix according to kevoree properties
                    // netmask should be properties
                    val command0 = array("/sbin/ifdown", ethname)
                    val process0 = Runtime.getRuntime().exec(command0)
                    readerOUTthread = Thread(Reader(process0.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process0.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process0.waitFor()

                    val command1 = array("/sbin/brctl", "addbr", bridgeName)
                    val process1 = Runtime.getRuntime().exec(command1)
                    readerOUTthread = Thread(Reader(process1.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process1.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process1.waitFor()

                    val command2 = array("/sbin/brctl", "addif", bridgeName, ethname)
                    val process2 = Runtime.getRuntime().exec(command2)
                    readerOUTthread = Thread(Reader(process2.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process2.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process2.waitFor()

                    val command3 = array("/sbin/ifconfig", ethname, "0.0.0.0")
                    val process3 = Runtime.getRuntime().exec(command3)
                    readerOUTthread = Thread(Reader(process3.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process3.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process3.waitFor()


                    val ip = ng.generateGW(null)!!
                    val command4 = array("/sbin/ifconfig", bridgeName, ip, "netmask", "255.255.255.0", "up")
                    val process4 = Runtime.getRuntime().exec(command4)
                    readerOUTthread = Thread(Reader(process4.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process4.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process4.waitFor()


                    //                val command5 = array("/sbin/ifconfig", "eth1","promisc", "up")
                    val command5 = array("/sbin/ifconfig", ethname, ip, "netmask", "255.255.255.0", "promisc", "up")
                    val process5 = Runtime.getRuntime().exec(command5)
                    readerOUTthread = Thread(Reader(process5.getInputStream()!!, modelElement.name!!, false))
                    readerERRthread = Thread(Reader(process5.getErrorStream()!!, modelElement.name!!, true))
                    readerOUTthread!!.start()
                    readerERRthread!!.start()
                    process5.waitFor()

                    val file = File(rootUserDirs.toString() + "/" + modelElement.name!! + "_rootfs/kevrun")
                    if (file.exists()) {
                        val bval = file.setExecutable(true);
                    }


                    if (!"".equals(routeditfname)) {
                        //echo 1 > /proc/sys/net/ipv4/ip_forward
                        val command6 = array("/bin/echo", ">", "1", "/proc/sys/net/ipv4/ip_forward")
                        val process6 = Runtime.getRuntime().exec(command6)
                        readerOUTthread = Thread(Reader(process6.getInputStream()!!, modelElement.name!!, false))
                        readerERRthread = Thread(Reader(process6.getErrorStream()!!, modelElement.name!!, true))
                        readerOUTthread!!.start()
                        readerERRthread!!.start()
                        process6.waitFor()


                        //iptables -t nat -A POSTROUTING -o wlan0 -j SNAT --to-source 10.20.41.39
                        val command7 = array("/sbin/iptables", "-t", "nat", "-A", "POSTROUTING", "-o", "wlan0", "-j", "SNAT", "--to-source", NodeManager.getAddressForItf(routeditfname)!!)
                        val process7 = Runtime.getRuntime().exec(command7)
                        readerOUTthread = Thread(Reader(process7.getInputStream()!!, modelElement.name!!, false))
                        readerERRthread = Thread(Reader(process7.getErrorStream()!!, modelElement.name!!, true))
                        readerOUTthread!!.start()
                        readerERRthread!!.start()
                        process7.waitFor()
                    }

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
        val process1 = Runtime.getRuntime().exec(args)
        val readerOUTthread1 = Thread(Reader(process1.getInputStream()!!, modelElement.name!!, false))
        val readerERRthread1 = Thread(Reader(process1.getErrorStream()!!, modelElement.name!!, true))
        readerOUTthread1.start()
        readerERRthread1.start()
        Runtime.getRuntime().exec(args).waitFor()
    }

    override fun destroy() {

        if (createBrdge) {
            val command1 = array("/sbin/ifconfig", this.hostitfname, "down")
            val process1 = Runtime.getRuntime().exec(command1)
            val readerOUTthread1 = Thread(Reader(process1.getInputStream()!!, modelElement.name!!, false))
            val readerERRthread1 = Thread(Reader(process1.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread1.start()
            readerERRthread1.start()
            process1.waitFor()

            val command2 = array("/sbin/ifconfig", bridgeName, "down")
            val process2 = Runtime.getRuntime().exec(command2)
            val readerOUTthread2 = Thread(Reader(process2.getInputStream()!!, modelElement.name!!, false))
            val readerERRthread2 = Thread(Reader(process2.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread2.start()
            readerERRthread2.start()
            process1.waitFor()

            val command3 = array("/sbin/brctl", "delif", this.hostitfname)
            val process3 = Runtime.getRuntime().exec(command3)
            val readerOUTthread3 = Thread(Reader(process3.getInputStream()!!, modelElement.name!!, false))
            val readerERRthread3 = Thread(Reader(process3.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread3.start()
            readerERRthread3.start()
            process3.waitFor()

            val command4 = array("/sbin/brctl", "delbr", bridgeName)
            val process4 = Runtime.getRuntime().exec(command3)
            val readerOUTthread4 = Thread(Reader(process4.getInputStream()!!, modelElement.name!!, false))
            val readerERRthread4 = Thread(Reader(process4.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread4.start()
            readerERRthread4.start()
            process4.waitFor()


            val command0 = array("/sbin/ifup", hostitfname)
            val process0 = Runtime.getRuntime().exec(command0)
            val readerOUTthread0 = Thread(Reader(process0.getInputStream()!!, modelElement.name!!, false))
            val readerERRthread0 = Thread(Reader(process0.getErrorStream()!!, modelElement.name!!, true))
            readerOUTthread0.start()
            readerERRthread0.start()
            process0.waitFor()

        }

        process?.destroy()

        readerOUTthread?.stop()
        readerERRthread?.stop()
    }
    private fun getJava(): String {
        val java_home: String? = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java"
    }
}