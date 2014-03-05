package org.kevoree.library.cloud.lightlxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.DictionaryValue;
import org.kevoree.serializer.JSONModelSerializer;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * Created by duke on 09/12/2013.
 */
public class ConfigGenerator {

    public final String[] baseDirNames = {"usr", "lib", "lib32" , "lib64", "opt","etc", "bin", "sbin", "proc", "var", "dev/pts", "dev/shm", "tmp","run"};

    public String generate(String nodeName, String ip, String gateway, String mac,String bridgeName,String baseRootDirs, String intfName) {

        String base = "lxc.utsname=${nodename}\n" +
                "lxc.network.type=veth\n" +
                "#lxc.network.hwaddr = ${mac}\n" +
                "lxc.devttydir =\n"+
                "lxc.tty = 2\n"+
                "lxc.pts = 1024\n"+
                "lxc.network.link=${bridgeName}\n" +
                "lxc.network.flags=up\n" +
                "lxc.network.name=${intfName} \n" +
                "lxc.network.ipv4 = ${ip}/24\n" +
                "lxc.network.ipv4.gateway = ${ip.gw}\n" +
                "lxc.rootfs = ${baseRootDirs}/${nodename}_rootfs\n" +
                 "# Allow any mknod (but not using the node)\n"+
                "lxc.cgroup.devices.allow = c *:* m\n"+
                "lxc.cgroup.devices.allow = b *:* m\n"+
                "# /dev/null and zero\n"+
                "lxc.cgroup.devices.allow = c 1:3 rwm\n"+
                "lxc.cgroup.devices.allow = c 1:5 rwm\n"+
                "# consoles\n"+
                "lxc.cgroup.devices.allow = c 5:1 rwm\n"+
                "lxc.cgroup.devices.allow = c 5:0 rwm\n"+
                "#lxc.cgroup.devices.allow = c 4:0 rwm\n"+
                "#lxc.cgroup.devices.allow = c 4:1 rwm\n"+
                "# /dev/{,u}random\n"+
                "lxc.cgroup.devices.allow = c 1:9 rwm\n"+
                "lxc.cgroup.devices.allow = c 1:8 rwm\n"+
                "lxc.cgroup.devices.allow = c 136:* rwm\n"+
                "lxc.cgroup.devices.allow = c 5:2 rwm\n"+
                "# rtc\n"+
                "lxc.cgroup.devices.allow = c 254:0 rwm\n"+
                "#fuse\n"+
                "lxc.cgroup.devices.allow = c 10:229 rwm\n"+
                "#tun\n"+
                "lxc.cgroup.devices.allow = c 10:200 rwm\n"+
                "#full\n"+
                "lxc.cgroup.devices.allow = c 1:7 rwm\n"+
                "#hpet\n"+
                "lxc.cgroup.devices.allow = c 10:228 rwm\n"+
                "#kvm\n"+
                "lxc.cgroup.devices.allow = c 10:232 rwm\n"+
                "lxc.mount.entry=/usr ${baseRootDirs}/${nodename}_rootfs/usr none ro,bind 0 0\n" +
                "lxc.mount.entry=/lib ${baseRootDirs}/${nodename}_rootfs/lib none ro,bind 0 0\n" +
                "lxc.mount.entry=/etc ${baseRootDirs}/${nodename}_rootfs/etc none ro,bind 0 0\n" +
                "lxc.mount.entry=/bin ${baseRootDirs}/${nodename}_rootfs/bin none ro,bind 0 0\n" +
                "lxc.mount.entry=/sbin ${baseRootDirs}/${nodename}_rootfs/sbin none ro,bind 0 0\n" +
                "lxc.mount.entry=proc ${baseRootDirs}/${nodename}_rootfs/proc proc nodev,noexec,nosuid 0 0 \n" +
                "lxc.mount.entry=/var ${baseRootDirs}/${nodename}_rootfs/var none ro,bind 0 0 \n" +
                "lxc.mount.entry = /dev/pts ${baseRootDirs}/${nodename}_rootfs/dev/pts devpts nosuid,noexec,mode=0620,ptmxmode=000,newinstance 0 0\n" +
                "lxc.mount.entry = /dev/shm ${baseRootDirs}/${nodename}_rootfs/dev/shm tmpfs nosuid,nodev,mode=1777 0 0\n" +
                "lxc.mount.entry = /tmp ${baseRootDirs}/${nodename}_rootfs/tmp tmpfs nosuid,nodev,mode=1777,size=1g 0 0\n";
            if (new File("/lib32").exists())
                base = base +  "lxc.mount.entry=/lib32 ${baseRootDirs}/${nodename}_rootfs/lib32 none ro,bind 0 0\n";
            if (new File("/run").exists())
                base = base +  "lxc.mount.entry=/run ${baseRootDirs}/${nodename}_rootfs/run none ro,bind 0 0\n";
            if (new File("/lib64").exists())
                base = base +  "lxc.mount.entry=/lib64 ${baseRootDirs}/${nodename}_rootfs/lib64 none ro,bind 0 0\n";
            if (new File("/opt").exists())
                base = base +  "lxc.mount.entry=/opt ${baseRootDirs}/${nodename}_rootfs/opt none ro,bind 0 0\n";
       //     new File(baseRootDirs+ "/"+ nodeName+"_rootfs/" + getJava().substring(0,getJava().lastIndexOf("/java")) ).mkdirs();
         //   base = base +  "lxc.mount.entry=" +getJava().substring(0,getJava().lastIndexOf("/java"))+" ${baseRootDirs}/${nodename}_rootfs"+ getJava().substring(0,getJava().lastIndexOf("/java"))+" none ro,bind 0 0\n";

        return base
                .replace("${nodename}", nodeName)
                .replace("${ip}", ip)
                .replace("${bridgeName}", bridgeName)
                .replace("${intfName}", intfName)
                .replace("${ip.gw}", gateway)
                .replace("${mac}", mac)
                .replace("${baseRootDirs}", baseRootDirs);
    }

    public File generateUserDir(File baseRootDirs, ContainerNode element, File platformJar,String bridgeName, String ip, String netmask, String gw, String intfName, String mac,Boolean sshdStart) throws IOException {
        //System.err.println(baseRootDirs.getAbsolutePath());
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

        MkNodeCommandExecutor.instance$.mkNode(newUserDir.getAbsolutePath(),element.getName());


        //generate the lxc config file
        File configLXC = new File(newUserDir, "config");
        FileWriter configLXCprinter = new FileWriter(configLXC);
        configLXCprinter.write(generate(element.getName(), ip, gw,mac,bridgeName,baseRootDirs.getAbsolutePath(), intfName));
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
        FileWriter runnerprinter = new FileWriter(runner);
        //set property
        runnerprinter.write("#!/bin/bash\n");
        String jrePath = System.getProperty("java.home");
        runnerprinter.write(" export PATH="+ jrePath+"/bin:$PATH\n");
        runnerprinter.write(" export JAVA_HOME="+ jrePath +"\n");
//        export PATH=/usr/lib/jvm/jdk1.8.0/bin:$PATH
        //export JAVA_HOME=/usr/lib/jvm/jdk1.8.0

        if (sshdStart)
            runnerprinter.write("/usr/sbin/dropbear -E -P /tmp/"+ element.getName()+ ".pid\n");

        runnerprinter.write("java");
        runnerprinter.write(" ");
        if (jvmArgs != null) {
            runnerprinter.write(jvmArgs);
            runnerprinter.write(" ");
        }
        runnerprinter.write("-Dnode.name=\"");
        runnerprinter.write(element.getName());
        runnerprinter.write("\" -Dnode.bootstrap=\"");
        runnerprinter.write("boot.json\" -jar runtime.jar");
        runnerprinter.write("\n");
        runnerprinter.flush();
        runnerprinter.close();

        if (!runner.setExecutable(true)) {
            throw new IOException("Unable to set executable bit on " + runner.getAbsolutePath());
        }
        return newUserDir;
    }

    private String getJava() {
        String java_home = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java";
    }

    private void copy(File source, File dest)
            throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null) {
            inputChannel.close();
            }
            if (outputChannel != null) {
            outputChannel.close();
            }
        }
    }

}
