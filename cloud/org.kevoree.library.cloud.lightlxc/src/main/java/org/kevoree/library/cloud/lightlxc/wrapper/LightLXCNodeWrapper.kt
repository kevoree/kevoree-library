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

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 09:22
 */

class LightLXCNodeWrapper(val modelElement: ContainerNode, override val targetObj: Any, override var tg: ThreadGroup, override val bs: BootstrapService, val routeditfname: String, val hostitfname: String,
                          val hostitfip: String, val containeripbaseaddress: String,
                          val createBrdge: Boolean, val bridgeName : String,val ipStep : Int , val ipStart : Int,val networkMask:String,val sshdStart : Boolean) : KInstanceWrapper {




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
                    Log.error("Can't download Kevoree platform, abording starting node "  + modelElement.name!! + " " + factory.getVersion())
                    return false
                }
                Log.info("Fork platform using {}", platformJar!!.getAbsolutePath())

                var rootUserDirs = Files.createTempDirectory("rootfs").toFile();
                //Log.error("go there")
                Log.info("file" + rootUserDirs?.getAbsolutePath())
                val ng = NetworkGenerator(this.containeripbaseaddress, this.hostitfip,ipStep ,ipStart )

                var cg = ConfigGenerator();

                val ethname = this.hostitfname
                var newUserDir = cg.generateUserDir(rootUserDirs, modelElement, platformJar, bridgeName, ng, routeditfname,sshdStart)

                if (createBrdge) {
                    // TODO check and fix according to kevoree properties
                    // netmask should be properties
                    val command0 = array("/sbin/ifdown", ethname)
                    MkNodeCommandExecutor.execute(modelElement.name!!,command0)

                    val command1 = array("/sbin/brctl", "addbr", bridgeName)
                    MkNodeCommandExecutor.execute(modelElement.name!!,command1)

                    val command2 = array("/sbin/brctl", "addif", bridgeName, ethname)
                    MkNodeCommandExecutor.execute(modelElement.name!!,command2)

                    val command3 = array("/sbin/ifconfig", ethname, "0.0.0.0")
                    MkNodeCommandExecutor.execute(modelElement.name!!,command3)


                    val ip = ng.generateGW(null)!!
                    val command4 = array("/sbin/ifconfig", bridgeName, ip, "netmask", networkMask, "up")
                    MkNodeCommandExecutor.execute(modelElement.name!!,command4)


                    //                val command5 = array("/sbin/ifconfig", "eth1","promisc", "up")
                    val command5 = array("/sbin/ifconfig", ethname, ip, "netmask", networkMask, "promisc", "up")
                    MkNodeCommandExecutor.execute(modelElement.name!!,command5)

                    val file = File(rootUserDirs.toString() + "/" + modelElement.name!! + "_rootfs/kevrun")
                    if (file.exists()) {
                        val bval = file.setExecutable(true);
                    }


                    if (!"".equals(routeditfname)) {
                        //echo 1 > /proc/sys/net/ipv4/ip_forward
                        val command6 = array("/bin/echo", ">", "1", "/proc/sys/net/ipv4/ip_forward")
                        MkNodeCommandExecutor.execute(modelElement.name!!,command6)


                        //iptables -t nat -A POSTROUTING -o wlan0 -j SNAT --to-source 10.20.41.39
                        val command7 = array("/sbin/iptables", "-t", "nat", "-A", "POSTROUTING", "-o", "wlan0", "-j", "SNAT", "--to-source", NodeManager.getAddressForItf(routeditfname)!!)
                        MkNodeCommandExecutor.execute(modelElement.name!!,command7)
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
        MkNodeCommandExecutor.execute(modelElement.name!!,args)
    }

    override fun destroy() {

        if (createBrdge) {
            val command1 = array("/sbin/ifconfig", this.hostitfname, "down")
            MkNodeCommandExecutor.execute(modelElement.name!!,command1)

            val command2 = array("/sbin/ifconfig", bridgeName, "down")
            MkNodeCommandExecutor.execute(modelElement.name!!,command2)

            val command3 = array("/sbin/brctl", "delif", this.hostitfname)
            MkNodeCommandExecutor.execute(modelElement.name!!,command3)

            val command4 = array("/sbin/brctl", "delbr", bridgeName)
            MkNodeCommandExecutor.execute(modelElement.name!!,command4)


            val command0 = array("/sbin/ifup", hostitfname)
            MkNodeCommandExecutor.execute(modelElement.name!!,command0)

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