package org.kevoree.library.cloud.lightlxc.wrapper;

import java.net.*;
import java.util.*;

/**
 * Created by root on 28/01/14.
 */
public class NodeManager {

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
