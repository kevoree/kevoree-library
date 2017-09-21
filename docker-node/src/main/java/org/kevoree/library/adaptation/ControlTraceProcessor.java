package org.kevoree.library.adaptation;

import com.github.dockerjava.api.DockerClient;
import org.kevoree.ContainerRoot;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.modeling.api.trace.ModelControlTrace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by leiko on 9/21/17.
 */
public class ControlTraceProcessor extends TraceProcessor {

    public ControlTraceProcessor(DockerClient docker, String nodeName, ContainerRoot currentModel, ContainerRoot targetModel) {
        super(docker, nodeName, currentModel, targetModel);
    }

    public List<AdaptationCommand> process(ModelControlTrace trace) {
        List<AdaptationCommand> cmds = new ArrayList<>();
        return cmds;
    }
}
