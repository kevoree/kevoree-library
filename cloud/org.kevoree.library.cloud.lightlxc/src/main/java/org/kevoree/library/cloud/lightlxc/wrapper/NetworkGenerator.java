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


    //18
    Integer ipStep;

    //1
    Integer ipStart;

    public NetworkGenerator(String baseIP, String gateway,Integer ipStep , Integer ipStart ){
     this.baseIP = baseIP;
     this.gateway= gateway;
     this.ipStep = ipStep;
     this.ipStart = ipStart;
    }

    public String generateIP(String  elementName) {
        Random rand = new Random();
        Integer ip = ipStart+rand.nextInt(ipStep);
        int i = 0;

        while (ips.contains(ip) && i<200){
            ip = ipStart+rand.nextInt(ipStep);
            i++;
        }
        if (i==200)
            return null;

            ips.add(ip);
            return baseIP + ip;
    }

    public String generateGW(String elementName) {
        return gateway;
    }

    public String generateMAC(String elementName) {
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
}
