package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.DictionaryValue;
import org.kevoree.serializer.JSONModelSerializer;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Created by duke on 09/12/2013.
 */
public class ConfigGenerator {

    public static final String[] baseDirNames = {"usr", "lib", "etc", "bin", "sbin", "proc", "var", "dev/pts", "dev/shm", "tmp"};

    public static String generate(String nodeName, String ip, String gateway, String mac) {

        String base = "lxc.utsname=${nodename}\n" +
                "lxc.network.type=veth\n" +
                "lxc.network.hwaddr = ${mac}\n" +
                "lxc.network.link=br0\n" +
                "lxc.network.flags=up\n" +
                "lxc.network.name=eth0 \n" +
                "lxc.network.ipv4 = ${ip}/24\n" +
                "lxc.network.ipv4.gateway = ${ip.gw}\n" +
                "lxc.rootfs = /${nodename}_rootfs\n" +
                "lxc.mount.entry=/usr /${nodename}_rootfs/usr none ro,bind 0 0\n" +
                "lxc.mount.entry=/lib /${nodename}_rootfs/lib none ro,bind 0 0\n" +
                "lxc.mount.entry=/etc /${nodename}_rootfs/etc none ro,bind 0 0\n" +
                "lxc.mount.entry=/bin /${nodename}_rootfs/bin none ro,bind 0 0\n" +
                "lxc.mount.entry=/sbin /sbin none ro,bind 0 0\n" +
                "lxc.mount.entry=proc /${nodename}_rootfs/proc proc nodev,noexec,nosuid 0 0 \n" +
                "lxc.mount.entry=/var /${nodename}_rootfs/var none ro,bind 0 0 \n" +
                "lxc.mount.entry = /dev/pts /${nodename}_rootfs/dev/pts devpts nosuid,noexec,mode=0620,ptmxmode=000,newinstance 0 0\n" +
                "lxc.mount.entry = /dev/shm /${nodename}_rootfs/dev/shm tmpfs nosuid,nodev,mode=1777 0 0\n" +
                "lxc.mount.entry = /tmp /${nodename}_rootfs/tmp tmpfs nosuid,nodev,noexec,mode=1777,size=1g 0 0";

        return base
                .replace("${nodename}", nodeName)
                .replace("${ip}", ip)
                .replace("${ip.gw}", gateway)
                .replace("${mac}", mac);

    }

    public static File generateUserDir(File baseRootDirs, ContainerNode element, File platformJar) throws IOException {
        if (!baseRootDirs.exists()) {
            baseRootDirs.mkdirs();
        }
        File newUserDir = new File(baseRootDirs, element.getName() + "_rootfs");
        if (!newUserDir.exists()) {
            newUserDir.mkdirs();
        }
        //copy the platform jar
        File platform = new File(newUserDir, "runtime.jar");
        copy(platformJar, platform);

        //copy the model in the new rootfs
        ContainerRoot baseModel = (ContainerRoot) element.eContainer();
        JSONModelSerializer jsonModelSerializer = new JSONModelSerializer();
        File config = new File(newUserDir, "boot.json");
        FileOutputStream fop = new FileOutputStream(config);
        jsonModelSerializer.serializeToStream(baseModel, fop);
        fop.flush();
        fop.close();
        //generate all directory for rootfs mount
        for (String rootFSDir : baseDirNames) {
            File sub = new File(newUserDir + File.separator + rootFSDir);
            if (!sub.exists()) {
                sub.mkdirs();
            }
        }
        //generate the lxc config file
        File configLXC = new File(newUserDir, "config");
        FileWriter configLXCprinter = new FileWriter(configLXC);
        configLXCprinter.write(generate(element.getName(), NetworkGenerator.generateIP(element), NetworkGenerator.generateGW(element),NetworkGenerator.generateGW(element)));
        configLXCprinter.flush();
        configLXCprinter.close();

        //generate the runner.sh
        String jvmArgs = null;
        if (element.getDictionary() != null) {
            DictionaryValue jvmArgsAttribute = element.getDictionary().findValuesByID("jvmArgs");
            if (jvmArgsAttribute != null) {
                jvmArgs = jvmArgsAttribute.toString();
            }
        }
        File runner = new File(newUserDir, "kevrun");
        runner.setExecutable(true);
        FileWriter runnerprinter = new FileWriter(runner);
        //set property
        runnerprinter.write("#!/bin/bash\n");

        runnerprinter.write(getJava());
        runnerprinter.write(" ");
        if (jvmArgs != null) {
            runnerprinter.write(jvmArgs);
        }
        runnerprinter.write(" ");
        runnerprinter.write("-Dnode.name=\"");
        runnerprinter.write(element.getName());
        runnerprinter.write("\" -Dnode.bootstrap=\"");
        runnerprinter.write("boot.json -jar runtime.jar");
        runnerprinter.write("\n");
        runnerprinter.flush();
        runnerprinter.close();
        return newUserDir;
    }

    private static String getJava() {
        String java_home = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java";
    }

    private static void copy(File source, File dest)
            throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

}