package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.ContainerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by duke on 09/12/2013.
 */
public class NetworkGenerator {


    private List<Integer> ips = new ArrayList<Integer>();

    //192.168.1.1
    String baseIP;
    //192.168.1.1
    String gateway;
    public NetworkGenerator(String baseIP, String gateway){
     this.baseIP = baseIP;
     this.gateway= gateway;
    }

    public String generateIP(ContainerNode element) {
        Random rand = new Random();
        Integer ip = 1+rand.nextInt(98);
        int i = 0;

        while (ips.contains(ip) && i<200){
            ip = 1+rand.nextInt(98);
            i++;
        }
        if (i==200)
            return null;

            ips.add(ip);
            return baseIP + ip;
    }

    public String generateGW(ContainerNode element) {
        return gateway;
    }

    public String generateMAC(ContainerNode element) {
        byte[] b = new byte[6];
        random.nextBytes(b);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (buffer.length() != 0) {
                buffer.append(":");
            }
            String end = String.format("%x", b[i]);
            if (end.length() == 1) {
                end = end + "0";
            }
            buffer.append(end);
        }
        return buffer.toString();
    }

    private static Random random = new Random();

    public static void main(String[] args) {
        NetworkGenerator ng = new NetworkGenerator("192.168.1.1","192.168.1.1");
        System.out.println( ng.generateMAC(null));
        System.out.println( ng.generateIP(null));
        System.out.println( ng.generateGW(null));


    }


}
