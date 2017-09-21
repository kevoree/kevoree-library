package org.kevoree.library;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.kevoree.ContainerRoot;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.handler.AbstractModelListener;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.service.ModelService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by leiko on 9/5/17.
 */
@NodeType(version = 2, description = "A Kevoree node to drive a Docker engine. Will automatically trigger " +
        "docker-2-model updates every \"<strong>update</strong>\" seconds. Defaults to 30s.")
public class DockerNode extends JavaNode {

    private DockerClient docker;
    private KevoreeFactory factory;
    private ModelListener modelListener;
    private ScheduledExecutorService executorService;

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    private Context context;

    @Param
    private long update = 30;

    public DockerNode() {
        super();
        this.factory = new DefaultKevoreeFactory();
    }

    @Start
    public void dockerStart() throws Exception {
        super.start();
        Log.info("DockerNode \"{}\" will update model based on local Docker engine every {}s", context.getNodeName(), update);
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.docker = DockerClientBuilder.getInstance(config).build();

        this.modelListener = new AbstractModelListener() {
            @Override
            public void updateSuccess(UpdateContext context) {
                if (executorService == null) {
                    executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.scheduleAtFixedRate(DockerNode.this::updateModelAccordingToDocker, 0, update, TimeUnit.SECONDS);
                }
            }
        };
        this.modelService.registerModelListener(this.modelListener);
    }


    @Stop
    public void dockerStop() throws Exception {
        super.stop();
        this.modelService.unregisterModelListener(this.modelListener);
        if (executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
        this.docker.close();
    }

    @Update
    public void dockerUpdate() {
        super.update();
        if (executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = Executors.newSingleThreadScheduledExecutor();
            this.executorService.scheduleAtFixedRate(
                    DockerNode.this::updateModelAccordingToDocker, 0, update, TimeUnit.SECONDS);
        }
    }

    /**
     * Converts the currentModel state of the Docker engine to a Kevoree model.
     * This model is then merged with the currentModel in-use model and deployed.
     *
     * Calling this method subsequently triggers a call to plan(currentModel, targetModel)
     */
    private void updateModelAccordingToDocker() {
        try {
            final ModelHelper modelHelper = new ModelHelper(context.getNodeName(), docker, factory);
            final ContainerRoot dockerModel = modelHelper.docker2model(modelService.getCurrentModel());
            modelService.update(dockerModel, e -> {
                if (e == null) {
                    Log.info("Model updated based on the local Docker engine configuration");
                } else {
                    Log.warn("Failed to update model based on the local Docker engine configuration", e);
                }
            });
        } catch (Exception e) {
            Log.warn("Failed to update model based on the local Docker engine configuration", e);
        }
    }

    /**
     * This is called when the node has to adapt from currentModel to targetModel.
     * The goal of this method is to return a list of executable adaptation commands according to
     * the delta between currentModel and targetModel.
     *
     * @param currentModel the currently in-use model
     * @param targetModel the model that we want to converge to
     * @return a list of commands to execute in order to go from currentModel to targetModel
     * @throws KevoreeAdaptationException when something goes wrong while planning adaptations
     */
    @Override
    public List<AdaptationCommand> plan(ContainerRoot currentModel, ContainerRoot targetModel)
            throws KevoreeAdaptationException {
        final List<AdaptationCommand> commands = super.plan(currentModel, targetModel);
        final List<AdaptationCommand> dockerCommands = new ModelHelper(context.getNodeName(), docker, factory)
                .model2docker(currentModel, targetModel);
        Log.debug("=== Docker commands ===");
        dockerCommands.forEach((cmd) -> Log.debug(" {} [{}]", cmd.getElement().path(), cmd.getType()));
        Log.debug("========================");
        commands.addAll(dockerCommands);
        return commands;
    }
}
