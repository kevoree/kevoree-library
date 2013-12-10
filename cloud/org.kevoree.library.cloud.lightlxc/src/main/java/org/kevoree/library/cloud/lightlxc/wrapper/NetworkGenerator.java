package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.ContainerNode;

/**
 * Created by duke on 09/12/2013.
 */
public class NetworkGenerator {

    public static String generateIP(ContainerNode element){
        return "192.168.1.110";
    }
    public static String generateGW(ContainerNode element){
        return "192.168.1.110";
    }
    public static String generateMAC(ContainerNode element){
        return "00:16:3e:74:f1:60";
    }

}
