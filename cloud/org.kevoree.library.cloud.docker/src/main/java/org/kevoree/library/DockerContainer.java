package org.kevoree.library;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.osgiresourcelocator.*;
import org.glassfish.hk2.osgiresourcelocator.ServiceLoader;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.external.generator.*;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.util.PortStreamer;
import org.kevoree.log.Log;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.spotify.docker.client.DockerClient.AttachParameter;

/**
 *
 * Created by leiko on 16/10/15.
 */
@ComponentType(description = "Runs a Docker container based on provided <strong>image</strong> name and the set of " +
        "options in the attributes: <strong>cmd</strong>, <strong>ports</strong>, <strong>removeOnClose</strong> " +
        "etc." +
        "<br/><br/>The <strong>cmd</strong> and <strong>ports</strong> attributes can specify several values by using" +
        "a <em>space</em> separator (ie. cmd: <em>ls -lArth</em>, ports: <em>80 9001:9000</em>)" +
        "<br/>By default, <strong>ports</strong> will forward the same port number from host to container. If you want" +
        "to differenciate ports, use the <em>hostPort:containerPort</em> syntax.")
public class DockerContainer {

    private String containerId;
    private DockerClient docker;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @KevoreeInject
    private Context context;

    @Param(optional = false)
    private String image;

    @Param
    private String cmd;

    @Param
    private String ports;

    @Param
    private String links;

    @Param(defaultValue = "true")
    private boolean removeOnStop = true;

    @Param
    private int stopTimeout = 5;

    @Param(defaultValue = "false")
    private boolean removeVolumes = false;

    @Output
    private Port stdout;

    @Output
    private Port stderr;

    @Start
    public void start() throws DockerCertificateException, DockerException, InterruptedException, IOException {
        this.docker = DefaultDockerClient.fromEnv().build();

        try {
            docker.inspectImage(this.image);
        } catch (ImageNotFoundException e) {
            docker.pull(this.image);
        }

        HostConfig hostConfig = HostConfig.builder()
                .portBindings(computePorts())
                .links(computeLinks())
                .build();

        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .attachStderr(true)
                .attachStdout(true)
                .attachStdin(true)
                .image(this.image)
                .cmd(computeCommands())
                .build();

        try {
            ContainerCreation creation = docker.createContainer(containerConfig);
            containerId = creation.id();

            computeAttach();

            try {
                docker.startContainer(containerId);
                Log.info("'{}' has started a new container '{}'", context.getInstanceName(), containerId);
            } catch (DockerException err) {
                Log.error("'{}' had a problem starting the container (are you sure your attributes are ok?)", context.getInstanceName());
            }

        } catch (DockerException e) {
            Log.error("'{}' had a problem creating the container (are you sure your attributes are ok?)", context.getInstanceName());
        }
    }

    @Stop
    public void stop() throws IOException {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();

        try {
            docker.stopContainer(containerId, stopTimeout);
        } catch (Exception ignored) {}

        try {
            docker.killContainer(containerId);
        } catch (Exception ignored) {}

        if (removeOnStop) {
            try {
                docker.removeContainer(containerId, removeVolumes);
            } catch (Exception ignored) {}
        }
    }

    @Update
    public void update() throws InterruptedException, DockerException, DockerCertificateException, IOException {
        this.stop();
        this.start();
    }

    @NotNull
    private List<String> computeCommands() {
        List<String> cmdList = new ArrayList<>();
        if (cmd != null && !cmd.isEmpty()) {
            cmdList = Arrays.asList(cmd.split(" "));
        }
        return cmdList;
    }

    @NotNull
    private Map<String, List<PortBinding>> computePorts() {
        List<String> portsList = new ArrayList<>();
        if (ports != null && !ports.isEmpty()) {
            portsList = Arrays.asList(ports.split(" "));
        }
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : portsList) {
            String hostPort = port,
                    containerPort = port;
            if (port.contains(":")) {
                String[] splitted = port.split(":");
                hostPort = splitted[0];
                containerPort = splitted[1];
            }

            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", containerPort));
            portBindings.put(hostPort, hostPorts);
        }
        return portBindings;
    }

    private List<String> computeLinks() {
        List<String> linksList = new ArrayList<>();
        if (links != null && !links.isEmpty()) {
            linksList = Arrays.asList(links.split(" "));
        }
        return linksList;
    }

    private void computeAttach() throws IOException {
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                docker.attachContainer(containerId,
                        AttachParameter.LOGS,
                        AttachParameter.STDOUT,
                        AttachParameter.STDERR,
                        AttachParameter.STREAM)
                        .attach(new PortStreamer(stdout), new PortStreamer(stderr));
                return null;
            }
        });
    }

    public static void main(String[] args) throws DockerCertificateException, DockerException, InterruptedException, IOException {
        DockerContainer c = new DockerContainer();
        c.image = "busybox:latest";
        c.cmd = "ls -lArth";
        c.ports = "80 22 9001:9000";
        c.removeOnStop = true;
        c.stdout = new Port() {
            @Override
            public void send(String s, Callback callback) {
                System.out.println("stdout>"+s);
            }

            @Override
            public String getPath() {
                return null;
            }

            @Override
            public int getConnectedBindingsSize() {
                return 0;
            }
        };

        c.start();
        c.stop();
    }
}
