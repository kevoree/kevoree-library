package org.kevoree.library;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.kevoree.Channel;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.util.ParamService;
import org.kevoree.library.util.PortStreamer;
import org.kevoree.library.util.PortsService;
import org.kevoree.log.Log;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * Created by leiko on 16/10/15.
 */
@ComponentType(version = 1, description = "Runs a Docker container based on provided <strong>image</strong> name and the set of " +
        "options in the attributes: <strong>cmd</strong>, <strong>ports</strong>, <strong>removeOnClose</strong> " +
        "etc." +
        "<br/><br/>Most of the \"multivalued\" attributes can specify several values by using " +
        "a <em>space</em> separator (ie. cmd: <em>ls -lArth</em>, ports: <em>80 9001:9000</em>, links: <em>foo:bar baz:bar</em>)" +
        "<br/>By default, <strong>ports</strong> will forward the same port number from host to container. If you want " +
        "to differentiate ports, use the <em>hostPort:containerPort</em> syntax.")
public class DockerContainer {

    private final ParamService paramService = new ParamService();
    private final PortsService portsService = new PortsService();
    private String containerId;
    private DockerClient docker;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @KevoreeInject
    private Context context;

    @Param(optional = false)
    private String image;

    @Param
    private String name;

    @Param
    private String cmd;

    @Param
    private String ports;

    @Param
    private String links;

    @Param
    private String dns;

    @Param
    private String volumesFrom;

    @Param
    private String environment;

    /**
     * Binds is also named Volume in the public API.
     */
    @Param
    private String binds;

    @Param
    private boolean removeOnStop = true;

    @Param(optional = false)
    private Integer stopTimeout = 5;

    @Param
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

        final Map<String, List<PortBinding>> portBindings = portsService.computePorts(ports);
        final HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .links(paramService.computeParamToList(links))
                .dns(computeDNS())
                .volumesFrom(paramService.computeParamToList(volumesFrom))
                .binds(paramService.computeParamToList(binds))
                .build();

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .attachStderr(true)
                .attachStdout(true)
                .attachStdin(true)
                .image(this.image)
                .cmd(paramService.computeParamToList(cmd))
                .exposedPorts(portBindings.keySet())
                .env(paramService.computeEnvs(environment))
                .build();

        try {
            final ContainerCreation creation;
            if (name != null && !name.isEmpty()) {
                creation = docker.createContainer(containerConfig, name);
            } else {
                creation = docker.createContainer(containerConfig);
            }
            containerId = creation.id();

            try {
                docker.startContainer(containerId);
                Log.info("'{}' has started a new container '{}'", context.getInstanceName(), containerId);

                computeAttach();
            } catch (DockerException err) {
                Log.error("'{}' had a problem starting the container (are you sure your attributes are ok?)", context.getInstanceName());
            }

        } catch (DockerException e) {
            Log.error("'{}' had a problem creating the container (are you sure your attributes are ok?)", context.getInstanceName());
        }
    }

    private List<String> computeDNS() {
        List<String> dnsList = new ArrayList<>();
        if (dns != null && !dns.isEmpty()) {
            dnsList = Arrays.asList(dns.split(" "));
        }
        return dnsList;
    }

    @Stop
    public void stop() throws IOException {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();

        try {
            docker.stopContainer(containerId, stopTimeout);
            Log.info("'{}' has stopped container '{}'", context.getInstanceName(), containerId);
        } catch (Exception e) {
            Log.error("Something went wrong while stopping Docker container {} in {}", e, containerId, context.getInstanceName());
        }

        try {
            docker.killContainer(containerId);
            Log.info("'{}' has killed container '{}'", context.getInstanceName(), containerId);
        } catch (Exception e) {
            Log.error("Something went wrong while killing Docker container {} in {}", e, containerId, context.getInstanceName());
        }

        if (removeOnStop) {
            try {
                DockerClient.RemoveContainerParam removeParams = DockerClient.RemoveContainerParam.removeVolumes(removeVolumes);
                docker.removeContainer(containerId, removeParams);
                Log.info("'{}' has removed container '{}'", context.getInstanceName(), containerId);
            } catch (Exception e) {
                Log.error("Something went wrong while removing Docker container {} in {}", e, containerId, context.getInstanceName());
            }
        }

        docker.close();
    }

    @Update
    public void update() throws InterruptedException, DockerException, DockerCertificateException, IOException {
        this.stop();
        this.start();
    }

    private void computeAttach() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    docker.attachContainer(containerId,
                            DockerClient.AttachParameter.LOGS,
                            DockerClient.AttachParameter.STDOUT,
                            DockerClient.AttachParameter.STDERR,
                            DockerClient.AttachParameter.STREAM)
                            .attach(new PortStreamer(stdout), new PortStreamer(stderr));
                } catch (Exception ignore) {}
            }
        });
    }

    public static void main(String[] args) throws DockerCertificateException, DockerException, InterruptedException, IOException {
        final DockerContainer c = new DockerContainer();
        c.image = "busybox:latest";
        c.cmd = "ping google.fr";
        c.removeOnStop = true;
        c.stdout = new Port() {
            @Override
            public void send(String payload) {
                this.send(payload, null);
            }

            @Override
            public void send(String s, Callback callback) {
                System.out.println("stdout>"+s);
            }

            @Override
            public String getPath() {
                return null;
            }

            @Override
            public Set<Channel> getChannels() {
                return new HashSet<>();
            }
        };
        c.context = new Context() {
            @Override
            public String getPath() {
                return "/nodes["+this.getNodeName()+"]/components["+this.getInstanceName()+"]";
            }

            @Override
            public String getNodeName() {
                return "node0";
            }

            @Override
            public String getInstanceName() {
                return "container";
            }
        };

        c.start();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.info("Gonna sleep 5s");
                    Thread.currentThread().sleep(5000);
                    Log.info("Woke up");

                    c.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}
