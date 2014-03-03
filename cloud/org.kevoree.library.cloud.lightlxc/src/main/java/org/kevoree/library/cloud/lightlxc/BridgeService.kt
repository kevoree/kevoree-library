package org.kevoree.library.cloud.lightlxc

import org.kevoree.library.cloud.lightlxc.wrapper.MkNodeCommandExecutor
import org.kevoree.library.cloud.lightlxc.wrapper.NodeManager

/**
 * Created by root on 03/03/14.
 */


class BridgeService(val createBridge: Boolean, val hostitfname: String, val nodeName: String,
                    val bridgeName: String, val networkMask: String, val routeditfname: String,
                    val gw: String){


    fun start() {

        if (createBridge) {
            // TODO check and fix according to kevoree properties
            // netmask should be properties
            val command0 = array("/sbin/ifdown", hostitfname)
            MkNodeCommandExecutor.execute(nodeName, command0)

            val command1 = array("/sbin/brctl", "addbr", bridgeName)
            MkNodeCommandExecutor.execute(nodeName, command1)

            val command2 = array("/sbin/brctl", "addif", bridgeName, hostitfname)
            MkNodeCommandExecutor.execute(nodeName, command2)

            val command3 = array("/sbin/ifconfig", hostitfname, "0.0.0.0")
            MkNodeCommandExecutor.execute(nodeName, command3)
            //val ng = NetworkGenerator(this.containeripbaseaddress, this.hostitfip,ipStep ,ipStart )

            val ip = gw;
            val command4 = array("/sbin/ifconfig", bridgeName, ip, "netmask", networkMask, "up")
            MkNodeCommandExecutor.execute(nodeName, command4)


            //                val command5 = array("/sbin/ifconfig", "eth1","promisc", "up")
            val command5 = array("/sbin/ifconfig", hostitfname, ip, "netmask", networkMask, "promisc", "up")
            MkNodeCommandExecutor.execute(nodeName, command5)



            if (!"".equals(routeditfname)) {
                //echo 1 > /proc/sys/net/ipv4/ip_forward
                val command6 = array("/bin/echo", ">", "1", "/proc/sys/net/ipv4/ip_forward")
                MkNodeCommandExecutor.execute(nodeName, command6)


                //iptables -t nat -A POSTROUTING -o wlan0 -j SNAT --to-source 10.20.41.39
                val command7 = array("/sbin/iptables", "-t", "nat", "-A", "POSTROUTING", "-o", "wlan0", "-j", "SNAT", "--to-source", NodeManager.getAddressForItf(routeditfname)!!)
                MkNodeCommandExecutor.execute(nodeName, command7)
            }

        }

    }

    fun stop() {

        if (createBridge) {
            val command1 = array("/sbin/ifconfig", this.hostitfname, "down")
            MkNodeCommandExecutor.execute(nodeName, command1)

            val command2 = array("/sbin/ifconfig", bridgeName, "down")
            MkNodeCommandExecutor.execute(nodeName, command2)

            val command3 = array("/sbin/brctl", "delif", this.hostitfname)
            MkNodeCommandExecutor.execute(nodeName, command3)

            val command4 = array("/sbin/brctl", "delbr", bridgeName)
            MkNodeCommandExecutor.execute(nodeName, command4)


            val command0 = array("/sbin/ifup", hostitfname)
            MkNodeCommandExecutor.execute(nodeName, command0)

        }

    }
}