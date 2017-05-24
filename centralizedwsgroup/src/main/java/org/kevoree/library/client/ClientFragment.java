package org.kevoree.library.client;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.GroupHelper;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.json.JSONModelLoader;
import org.kevoree.modeling.api.json.JSONModelSerializer;

/**
 *
 * Created by leiko on 1/17/17.
 */
public class ClientFragment {

    private boolean registered;
    private CentralizedWSGroup instance;

    public ClientFragment(CentralizedWSGroup instance) {
        this.registered = false;
        this.instance = instance;
    }

    public Protocol.RegisterMessage register() {
        String nodeName = instance.getContext().getNodeName();
        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelSerializer serializer = factory.createJSONSerializer();
        String modelStr = serializer.serialize(instance.getModel());

        // register message
        return new Protocol.RegisterMessage(nodeName, modelStr);
    }

    public void push(Protocol.PushMessage msg) {
        if (registered) {
            KevoreeFactory factory = new DefaultKevoreeFactory();
            JSONModelLoader loader = factory.createJSONLoader();

            try {
                ContainerRoot model = (ContainerRoot) loader.loadModelFromString(msg.getModel()).get(0);
                Log.info("[{}][client] new model pushed by master \"{}\"", instance.getName(), getMasterNodeName());
                instance.getModelService().update(model);
            } catch (Exception e) {
                Log.warn("[{}][client] erroneous model received by master \"{}\" (push ignored)",
                        instance.getName(), getMasterNodeName());
            }
        } else {
            Log.warn("[{}][client] new model pushed by master \"{}\" ignored (state: unregistered)",
                    instance.getName(),  getMasterNodeName());
        }
    }

    public void registered() {
        registered = true;
        Log.info("[{}][client] registered on master \"{}\"", instance.getName(), getMasterNodeName());
    }

    public void unregister() {
        registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    private String getMasterNodeName() {
        Group group = (Group) instance.getModel().findByPath(instance.getContext().getPath());
        ContainerNode masterNode = GroupHelper.findMasterNode(group);
        if (masterNode != null) {
            return masterNode.getName();
        }

        return null;
    }
}
