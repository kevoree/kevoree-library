package org.kevoree.library.adaptation;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import org.kevoree.*;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.library.ModelHelper;
import org.kevoree.library.command.CreateContainer;
import org.kevoree.library.command.RemoveContainer;
import org.kevoree.library.command.StartContainer;
import org.kevoree.library.command.StopContainer;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.trace.ModelAddAllTrace;
import org.kevoree.modeling.api.trace.ModelAddTrace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by leiko on 9/21/17.
 */
public class AddTraceProcessor extends TraceProcessor {

    private static final String COMPONENTS = "components";
    private static final String TYPE_DEFINITION = "typeDefinition";

    public AddTraceProcessor(DockerClient docker, String nodeName, ContainerRoot currentModel, ContainerRoot targetModel) {
        super(docker, nodeName, currentModel, targetModel);
    }

    public List<AdaptationCommand> process(ModelAddTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();

        if (trace.getRefName().equals(COMPONENTS)) {
            Instance instance = (Instance) targetModel.findByPath(trace.getPreviousPath());
            if (ModelHelper.isDockerRelated(instance)) {
                if (!ModelHelper.isCreated(docker, instance)) {
                    log(trace);
                    cmds.add(new CreateContainer(docker, instance));
                    cmds.add(new StartContainer(docker, instance));
                }
            }
        } else if (trace.getRefName().equals(TYPE_DEFINITION)) {
            final KMFContainer elem = targetModel.findByPath(trace.getSrcPath());
            if (elem instanceof Instance) {
                final Instance current = (Instance) currentModel.findByPath(trace.getSrcPath());
                if (current != null) {
                    // if current is not null it means that we actually need to update the tdef of the instance
                    // otherwise it is just the AddTrace resulting from the creation of the instance
                    // which is handled previously by "components"
                    final Instance target = (Instance) elem;
                    if (ModelHelper.isDockerRelated(target)) {
                        cmds.add(new StopContainer(docker, current));
                        cmds.add(new RemoveContainer(docker, current));
                        cmds.add(new CreateContainer(docker, target));
                        cmds.add(new StartContainer(docker, target));
                        log(trace);
                    }
                }
            }
        }

        return cmds;
    }

    public List<AdaptationCommand> process(ModelAddAllTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();
        return cmds;
    }

    private void log(ModelAddTrace trace) {
        System.out.println("=== ADD TRACE ===");
        System.out.println(" s: " + trace.getSrcPath());
        System.out.println(" r: " + trace.getRefName());
        System.out.println(" p: " + trace.getPreviousPath());
    }
}
