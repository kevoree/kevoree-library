package org.kevoree.library.java.haproxy;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;

import java.io.File;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 18:43
 */
@ComponentType
public class HAProxy {

    @Param
    Integer listeningPort = 8080;

    @KevoreeInject
    Context context;

    @Param
    String config = "";

    private Process process;
    private Thread readerOUTthread;
    private Thread readerERRthread;

    @Start
    public void start() throws Exception {
        File executable = File.createTempFile("haproxy_" /*+ context.getNodeName() + "_" + context.getInstanceName()*/, "");
        if (OSHelper.isMac()) {
            OSHelper.copy(this.getClass().getClassLoader().getResourceAsStream("mac_haproxy"), executable);
        } else {
            if (OSHelper.isUnix()) {
                OSHelper.copy(this.getClass().getClassLoader().getResourceAsStream("nux_haproxy"), executable);
            } else {
                throw new Exception("Unsupported platform");
            }
        }
        executable.setExecutable(true);
        executable.deleteOnExit();
        process = Runtime.getRuntime().exec(executable.getAbsolutePath());
        readerOUTthread = new Thread(new Reader(process.getInputStream()));
        readerERRthread = new Thread(new Reader(process.getErrorStream()));
        readerOUTthread.start();
        readerERRthread.start();
    }

    @Update
    public void update() throws Exception {
        stop();
        start();
    }

    @Stop
    public void stop() {
        process.destroy();
        readerOUTthread.stop();
        readerERRthread.stop();
    }

    public static void main(String[] args) throws Exception {
        HAProxy proxy = new HAProxy();
        proxy.start();
    }

    public String generateConfig() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(OSHelper.read(this.getClass().getClassLoader().getResourceAsStream("base.cfg")));
        buffer.append("\n");



        return buffer.toString();
    }

}
