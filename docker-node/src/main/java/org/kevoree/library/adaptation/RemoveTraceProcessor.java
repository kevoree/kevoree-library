package org.kevoree.library.adaptation;

import com.github.dockerjava.api.DockerClient;
import org.kevoree.ContainerRoot;
import org.kevoree.Instance;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.library.ModelHelper;
import org.kevoree.library.command.RemoveContainer;
import org.kevoree.library.command.StopContainer;
import org.kevoree.modeling.api.trace.ModelRemoveAllTrace;
import org.kevoree.modeling.api.trace.ModelRemoveTrace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by leiko on 9/21/17.
 */
public class RemoveTraceProcessor extends TraceProcessor {

    private static final String COMPONENTS = "components";

    public RemoveTraceProcessor(DockerClient docker, String nodeName, ContainerRoot currentModel, ContainerRoot targetModel) {
        super(docker, nodeName, currentModel, targetModel);
    }

    public List<AdaptationCommand> process(ModelRemoveTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();

        if (trace.getRefName().equals(COMPONENTS)) {
            Instance instance = (Instance) currentModel.findByPath(trace.getObjPath());
            if (ModelHelper.isDockerRelated(instance)) {
                if (ModelHelper.isCreated(docker, instance)) {
                    log(trace);
                    cmds.add(new StopContainer(docker, instance));
                    cmds.add(new RemoveContainer(docker, instance));
                }
            }
        }

        return cmds;
    }

    public List<AdaptationCommand> process(ModelRemoveAllTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();
        return cmds;
    }

    private void log(ModelRemoveTrace trace) {
        System.out.println("=== REMOVE TRACE ===");
        System.out.println(" s: " + trace.getSrcPath());
        System.out.println(" r: " + trace.getRefName());
        System.out.println(" o: " + trace.getObjPath());
    }
}
