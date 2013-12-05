package org.kevoree.library.java.haproxy;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.library.java.haproxy.api.Backend;
import org.kevoree.library.java.haproxy.api.Server;
import org.kevoree.log.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 18:43
 */
@ComponentType
public class HAProxy implements ModelListener {

    @Param
    Integer listeningPort = 8080;

    @KevoreeInject
    Context context;

    @Param
    String config = "";

    private Process process;
    private Thread readerOUTthread;
    private Thread readerERRthread;

    @KevoreeInject
    ModelService modelService;

    File configFile = null;
    File executable = null;

    @Start
    public void start() throws Exception {
        this.modelService.registerModelListener(this);
        executable = File.createTempFile("haproxy_" /*+ context.getNodeName() + "_" + context.getInstanceName()*/, "");
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

        configFile = File.createTempFile("haproxy_config" /*+ context.getNodeName() + "_" + context.getInstanceName()*/, "");
        configFile.deleteOnExit();
        generateConfig(configFile, modelService.getCurrentModel().getModel());

        String[] params = {executable.getAbsolutePath(), "-f", configFile.getAbsolutePath()};
        process = Runtime.getRuntime().exec(params);
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
        this.modelService.unregisterModelListener(this);
        process.destroy();
        readerOUTthread.stop();
        readerERRthread.stop();
    }

    public static void main(String[] args) throws Exception {
        HAProxy proxy = new HAProxy();
        proxy.start();
    }

    //TODO cleanup this ...
    public void generateConfig(File configFile, ContainerRoot model) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(OSHelper.read(this.getClass().getClassLoader().getResourceAsStream("base.cfg")));
        buffer.append("\n");
        HashMap<String, Backend> backends = new HashMap<String, Backend>();
        for (ContainerNode node : model.getNodes()) {
            for (ComponentInstance instance : node.getComponents()) {
                if (instance.getTypeDefinition().getDictionaryType() != null
                        && instance.getTypeDefinition().getDictionaryType().findAttributesByID("http_port") != null
                        && instance.getStarted()
                        && !instance.getName().equals(context.getInstanceName())) {

                    if (!backends.containsKey(instance.getTypeDefinition().getName())) {
                        Backend backend = new Backend();
                        backends.put(instance.getTypeDefinition().getName(), backend);
                    }
                    Backend backend = backends.get(instance.getTypeDefinition().getName());
                    backend.setName(instance.getTypeDefinition().getName());
                    Server s = new Server();
                    s.setIp("127.0.0.1");
                    s.setName(instance.getName());
                    s.setPort(instance.getDictionary().findValuesByID("http_port").getValue());
                    backend.getServers().add(s);
                }
            }
        }

        if (backends.size() > 0) {
            buffer.append("default_backend ");
            String firstKey = backends.keySet().iterator().next();
            buffer.append(backends.get(firstKey).getName());
            buffer.append("\n");
        }
        for (String key : backends.keySet()) {
            buffer.append("\n");
            buffer.append(backends.get(key));
            buffer.append("\n");
        }
        FileWriter writer = new FileWriter(configFile);
        writer.write(buffer.toString());
        writer.close();
    }

    @Override
    public boolean preUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        return true;
    }

    @Override
    public boolean initUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        return true;
    }

    @Override
    public boolean afterLocalUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        try {
            Log.info("Regenerate config " + configFile.getAbsolutePath());
            generateConfig(configFile, proposedModel);

            process.destroy();
            readerOUTthread.stop();
            readerERRthread.stop();
            String[] params = {executable.getAbsolutePath(), "-f", configFile.getAbsolutePath()};
            process = Runtime.getRuntime().exec(params);
            readerOUTthread = new Thread(new Reader(process.getInputStream()));
            readerERRthread = new Thread(new Reader(process.getErrorStream()));
            readerOUTthread.start();
            readerERRthread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void modelUpdated() {
    }

    @Override
    public void preRollback(ContainerRoot currentModel, ContainerRoot proposedModel) {
    }

    @Override
    public void postRollback(ContainerRoot currentModel, ContainerRoot proposedModel) {
    }
}
