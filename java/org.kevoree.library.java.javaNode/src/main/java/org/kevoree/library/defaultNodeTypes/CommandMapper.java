package org.kevoree.library.defaultNodeTypes;

import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.MBinding;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.defaultNodeTypes.command.*;
import org.kevoree.library.defaultNodeTypes.planning.JavaPrimitive;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 1/29/13
 * Time: 11:22 AM
 */
public class CommandMapper {

    java.util.ArrayList<EndAwareCommand> toClean = new java.util.ArrayList<EndAwareCommand>();

    public CommandMapper(Map<String, Object> registry) {
        this.registry = registry;
    }

    public void doEnd() {
        for (EndAwareCommand cmd : toClean) {
            cmd.doEnd();
        }
        toClean.clear();
    }

    protected Map<String, Object> registry;


    public PrimitiveCommand buildPrimitiveCommand(org.kevoreeadaptation.AdaptationPrimitive p, String nodeName, BootstrapService bs) {
        String pTypeName = p.getPrimitiveType();
        if (pTypeName.equals(JavaPrimitive.UpdateDictionaryInstance.name())) {
            if (((Instance) p.getRef()).getName().equals(nodeName)) {
                return new SelfDictionaryUpdate((Instance) p.getRef(), registry);
            } else {
                return new UpdateDictionary((Instance) p.getRef(), nodeName, registry);
            }
        }
        if (pTypeName.equals(JavaPrimitive.StartInstance.name())) {
            return new StartStopInstance((Instance) p.getRef(), nodeName, true, registry, bs);
        }
        if (pTypeName.equals(JavaPrimitive.StopInstance.name())) {
            return new StartStopInstance((Instance) p.getRef(), nodeName, false, registry, bs);
        }
        if (pTypeName.equals(JavaPrimitive.AddBinding.name())) {
            return new AddBindingCommand((MBinding) p.getRef(), nodeName, registry);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveBinding.name())) {
            return new RemoveBindingCommand((MBinding) p.getRef(), nodeName, registry);
        }
        if (pTypeName.equals(JavaPrimitive.AddDeployUnit.name())) {
            return new AddDeployUnit((DeployUnit) p.getRef(), bs, registry);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveDeployUnit.name())) {
            RemoveDeployUnit res = new RemoveDeployUnit((DeployUnit) p.getRef(), bs, registry);
            toClean.add(res);
            return res;
        }
        if (pTypeName.equals(JavaPrimitive.UpdateDeployUnit.name())) {
            UpdateDeployUnit res = new UpdateDeployUnit((DeployUnit) p.getRef(), bs, registry);
            toClean.add(res);
            return res;
        }
        if (pTypeName.equals(JavaPrimitive.AddInstance.name())) {
            return new AddInstance((Instance) p.getRef(), nodeName, registry, bs);
        }
        if (pTypeName.equals(JavaPrimitive.RemoveInstance.name())) {
            return new RemoveInstance((Instance) p.getRef(), nodeName, registry, bs);
        }
        return new NoopCommand();
    }


}
