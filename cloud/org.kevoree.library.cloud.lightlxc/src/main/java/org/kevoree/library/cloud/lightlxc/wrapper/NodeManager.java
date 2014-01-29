package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.defaultNodeTypes.wrapper.NodeWrapper;
import org.kevoree.log.Log;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;

/**
 * Created by root on 28/01/14.
 */
public class NodeManager {

    public static void main(String args[]) throws SocketException {
        System.out.println(getAddressForItf("wlan0"));
    }

    static String getInterfaceInformation(NetworkInterface netint) throws SocketException {
        List<InetAddress> inetAddresses = Collections.list(netint.getInetAddresses());
        if (inetAddresses.size()>0){
            String ip = inetAddresses.get(inetAddresses.size() - 1).toString();
            return ip.substring(1,ip.length());
        }
        return null;
    }

    public static String getAddressForItf(String itfName) throws SocketException{
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        String ip = null;
        for (NetworkInterface netint : Collections.list(nets)){
            if (itfName.equals(netint.getName())){
                ip=getInterfaceInformation(netint);
                break;
            }
        }
        return ip;

    }

}
