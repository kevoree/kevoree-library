package org.kevoree.library.adaptation;

import com.github.dockerjava.api.DockerClient;
import org.kevoree.*;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.library.ModelHelper;
import org.kevoree.library.command.StartContainer;
import org.kevoree.library.command.StopContainer;
import org.kevoree.library.command.UpdateContainer;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.trace.ModelSetTrace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by leiko on 9/21/17.
 */
public class SetTraceProcessor extends TraceProcessor {

    private static final String STARTED = "started";
    private static final String VALUE = "value";

    public SetTraceProcessor(DockerClient docker, String nodeName, ContainerRoot currentModel, ContainerRoot targetModel) {
        super(docker, nodeName, currentModel, targetModel);
    }

    public List<AdaptationCommand> process(ModelSetTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();

        if (trace.getRefName().equals(STARTED)) {
            final KMFContainer elem = targetModel.findByPath(trace.getSrcPath());
            if (elem instanceof ComponentInstance) {
                ComponentInstance instance = (ComponentInstance) elem;
                if (((ContainerNode) instance.eContainer()).getName().equals(nodeName)) {
                    ContainerNode node = targetModel.findNodesByID(nodeName);
                    // only start/stop if node platform is not stopping
                    if (node.getStarted()) {
                        boolean isRunning = ModelHelper.isRunning(docker, instance);
                        boolean doStart = trace.getContent().toLowerCase().equals("true");
                        if (doStart) {
                            // start behavior
                            if (!isRunning) {
                                log(trace);
                                cmds.add(new StartContainer(docker, instance));
                            }
                        } else {
                            // stop behavior
                            if (isRunning) {
                                log(trace);
                                cmds.add(new StopContainer(docker, instance));
                            }
                        }
                    }
                }
            }
        } else if (trace.getRefName().equals(VALUE)) {
            final Value elem = (Value) targetModel.findByPath(trace.getSrcPath());
            if (elem.eContainer() instanceof Dictionary) {
                Instance updatedInstance = (Instance) elem.eContainer().eContainer();
                Instance currentInstance = (Instance) currentModel.findByPath(updatedInstance.path());
                if (currentInstance != null) {
                    if (ModelHelper.isDockerRelated(updatedInstance)) {
                        log(trace);
                        cmds.add(new UpdateContainer(docker, currentInstance, updatedInstance));
                    }
                }
            }
        }

        return cmds;
    }

    private void log(ModelSetTrace trace) {
        System.out.println("=== SET TRACE ===");
        System.out.println(" s: " + trace.getSrcPath());
        System.out.println(" r: " + trace.getRefName());
        System.out.println(" o: " + trace.getObjPath());
        System.out.println(" v: " + trace.getContent());
    }
}
