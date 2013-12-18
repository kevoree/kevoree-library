package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.ContainerNode;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by duke on 09/12/2013.
 */
public class NetworkGenerator {

    public static String generateIP(ContainerNode element) {
        return "192.168.1.110";
    }

    public static String generateGW(ContainerNode element) {
        return "192.168.1.110";
    }

    public static String generateMAC(ContainerNode element) {
        byte[] b = new byte[6];
        random.nextBytes(b);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (buffer.length() != 0) {
                buffer.append("-");
            }
            String end = String.format("%x", b[i]);
            if (end.length() == 1) {
                end = end+"0";
            }
            buffer.append(end);
        }
        return buffer.toString();
    }

    private static Random random = new Random();

    public static void main(String[] args) {

    }


}
