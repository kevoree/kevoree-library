package org.kevoree.library.java.wrapper;

import org.kevoree.Value;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileOutputStream;
import org.kevoree.api.BootstrapService;
import org.kevoree.ContainerRoot;
import org.kevoree.ContainerNode;
import org.kevoree.log.Log;

import java.net.*;
import java.util.HashSet;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.kevoree.factory.DefaultKevoreeFactory;

import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 17/11/2013
 * Time: 20:03
 */

public class NodeWrapper extends KInstanceWrapper {

    private Process process = null;
    private Thread readerOUTthread = null;
    private Thread readerERRthread = null;
    private JSONModelSerializer modelSaver = new DefaultKevoreeFactory().createJSONSerializer();
    private File tempFile = null;
    private int adminPort;

    public NodeWrapper(ContainerNode modelElement, Object targetObj, String nodeName, ThreadGroup tg, BootstrapService bs) throws UnknownHostException {
        setModelElement(modelElement);
        setTargetObj(targetObj);
        setNodeName(nodeName);
        setTg(tg);
        setBs(bs);
    }


    private class Reader implements Runnable {
        private InputStream inputStream;
        private String nodeName;
        private boolean error;
        private BufferedReader br;

        private Reader(InputStream inputStream, String nodeName, boolean error) {
            this.inputStream = inputStream;
            this.nodeName = nodeName;
            this.error = error;
            br = new BufferedReader(new InputStreamReader(inputStream));
        }


        public void run() {
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
                    line = nodeName + "/" + line;
                    if (error) {
                        System.err.println(line);
                    } else {
                        System.out.println(line);
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public boolean kInstanceStart(ContainerRoot tmodel) {
        if (!getIsStarted()) {

            HashSet<String> urls = new HashSet<String>();
            urls.add("http://repo1.maven.org/maven2");
            String version = new DefaultKevoreeFactory().getVersion();
            if (version.toString().contains("SNAPSHOT")) {
                urls.add("http://oss.sonatype.org/content/groups/public/");
            }
            File platformJar = getBs().resolve("mvn:org.kevoree.platform:org.kevoree.platform.standalone:" + version, urls);
            if (platformJar == null) {
                Log.error("Can't download Kevoree platform, abording starting node");
                return false;
            }
            String jvmArgs = null;
            if (getModelElement().getDictionary() != null) {
                Value jvmArgsAttribute = getModelElement().getDictionary().findValuesByID("jvmArgs");
                if (jvmArgsAttribute != null) {
                    jvmArgs = jvmArgsAttribute.toString();
                }
            }
            Log.debug("Fork platform using {}", platformJar.getAbsolutePath());
            try {
                tempFile = File.createTempFile("bootModel" + getModelElement().getName(), ".json");
                FileOutputStream tempIO = new FileOutputStream(tempFile);
                modelSaver.serializeToStream(tmodel, tempIO);
                tempIO.close();
                tempIO.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String classPath = System.getProperty("java.class.path");
            StringBuilder newClassPath = new StringBuilder();
            String[] classPathList = classPath.split(":");
            newClassPath.append(platformJar.getAbsolutePath());
            for(String cpe : classPathList) {
                if (!cpe.contains("org.kevoree.platform.standalone-")) {
                    newClassPath.append(File.pathSeparator);
                    newClassPath.append(cpe);
                }
            }

            String devOption = "-Dkevoree.prod=true";
            if (System.getProperty("kevoree.dev") != null) {
                devOption = "-Dkevoree.dev=" + System.getProperty("kevoree.dev");
            }

            adminPort = FreeSocketDetector.detect(50000, 60000);

            String[] execArray = {getJava(), "-cp", newClassPath.toString(), devOption, "-Dnode.admin=" + adminPort, "-Dnode.bootstrap=" + tempFile.getAbsolutePath(), "-Dnode.name=" + getModelElement().getName(), "org.kevoree.platform.standalone.App"};
            if (jvmArgs != null) {
                String[] newArray = {getJava(), jvmArgs, "-cp", newClassPath.toString(), devOption, "-Dnode.admin=" + adminPort, "-Dnode.bootstrap=" + tempFile.getAbsolutePath(), "-Dnode.name=" + getModelElement().getName(), "org.kevoree.platform.standalone.App"};
                execArray = newArray;
            }

            try {
                process = Runtime.getRuntime().exec(execArray);
                readerOUTthread = new Thread(new Reader(process.getInputStream(), getModelElement().getName(), false));
                readerERRthread = new Thread(new Reader(process.getErrorStream(), getModelElement().getName(), true));
                readerOUTthread.start();
                readerERRthread.start();
                setIsStarted(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }



    public boolean kInstanceStop(ContainerRoot tmodel) {
        if (getIsStarted()) {

            DatagramSocket clientSocket = null;
            try {
                clientSocket = new DatagramSocket();
                InetAddress iPAddress = InetAddress.getByName("localhost");
                byte[] payload = "stop".getBytes(Charset.defaultCharset());
                DatagramPacket sendPacket = new DatagramPacket(payload, payload.length, iPAddress, adminPort);
                clientSocket.send(sendPacket);

                process.waitFor();
                readerOUTthread.interrupt();
                readerERRthread.interrupt();
                tempFile.delete();
                setIsStarted(false);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
        return true;
    }

    @Override
    public void create() {

    }

    @Override
    public void destroy() {

    }

    private String getJava() {
        String java_home = System.getProperty("java.home");
        return java_home + File.separator + "bin" + File.separator + "java";
    }
}