package org.kevoree.library.server;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.GroupHelper;
import org.kevoree.library.util.ModelReducer;
import org.kevoree.library.util.ShortId;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.ModelCloner;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.util.HashMap;

/**
 *
 * Created by leiko on 1/17/17.
 */
public class ServerFragment {

    private CentralizedWSGroup instance;
    private HashMap<String, String> names;
    private HashMap<String, String> ids;

    public ServerFragment(CentralizedWSGroup instance) {
        this.instance = instance;
        this.names = new HashMap<>();
        this.ids = new HashMap<>();
    }

    public String register(Protocol.RegisterMessage msg) {
        String id = names.get(msg.getNodeName());

        if (id != null) {
            Log.warn("[{}][master] node \"{}\" is already registered (id={})",
                    instance.getName(), msg.getNodeName(),id);
        } else {
            id = ShortId.gen();
            this.names.put(msg.getNodeName(), id);
            this.ids.put(id, msg.getNodeName());
            Log.info("[{}][master] node \"{}\" registered (id={})", instance.getName(), msg.getNodeName(), id);

            KevoreeFactory factory = new DefaultKevoreeFactory();
            JSONModelLoader loader = factory.createJSONLoader();
            ContainerRoot model = null;

            try {
                model = (ContainerRoot) loader.loadModelFromString(msg.getModel()).get(0);
            } catch (Exception e) {
                Log.warn("[{}][master] erroneous model received from \"{}\" registration",
                        instance.getName(), msg.getNodeName());
                Log.warn(e.toString());
            }

            if (model != null) {
                ContainerRoot currentModel = instance.getModelService().getCurrentModel().getModel();
                Group group = (Group) currentModel.findByPath(instance.getContext().getPath());
                ContainerNode masterNode = GroupHelper.findMasterNode(group);
                Log.debug("[{}][master] reducing register model for master \"{}\" and client \"{}\"", instance.getName(),
                        masterNode.getName(), msg.getNodeName());
                ContainerRoot registerModel = ModelReducer.reduce(model, masterNode.getName(), msg.getNodeName());
                ModelCompare compare = factory.createModelCompare();
                compare.merge(registerModel, currentModel).applyOn(registerModel);

                // updating current core model with the model from registered node
                instance.getModelService().update(registerModel, null);
            }
        }

        return id;
    }

    public void push(String address, Protocol.PushMessage msg) {
        Log.info("[{}][master] push issued by {}", instance.getName(), address);
        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelLoader loader = factory.createJSONLoader();

        try {
            ContainerRoot model = (ContainerRoot) loader.loadModelFromString(msg.getModel()).get(0);
            instance.getModelService().update(model, null);
        } catch (Exception e) {
            Log.warn("[{}][master] erroneous model received (push ignored)", instance.getName());
        }
    }

    public String pull(String id) {
        String name = this.ids.get(id);
        if (name == null) {
            Log.info("[{}][master] pull requested by {}", instance.getName(), id);
        } else {
            Log.info("[{}][master] pull requested by registered node {} (id={})", instance.getName(), name, id);
        }

        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelSerializer serializer = factory.createJSONSerializer();

        return serializer.serialize(instance.getModelService().getCurrentModel().getModel());
    }

    public void close(String id, boolean withOnDisconnect) {
        String name = this.ids.remove(id);
        if (name == null) {
            Log.debug("[{}][master] client {} disconnected", instance.getName(), id);
        } else {
            this.names.remove(name);
            Log.debug("[{}][master] node \"{}\" disconnected (id={})", instance.getName(), name, id);
            if (withOnDisconnect) {
                // a registered node disconnected from master => execute onDisconnect kevscript if any
                String onDisconnectKevs = instance.getOnDisconnect().trim();
                if (!onDisconnectKevs.isEmpty()) {
                    onDisconnectKevs = kevsTpl(onDisconnectKevs, name);
                    Log.debug("[{}][master] submitting onDisconnect KevScript:", instance.getName());
                    Log.debug(onDisconnectKevs);
                    KevoreeFactory factory = new DefaultKevoreeFactory();
                    ModelCloner cloner = factory.createModelCloner();
                    ContainerRoot model = cloner.clone(instance.getModel());
                    try {
                        instance.getKevsService().execute(onDisconnectKevs, model);
                        instance.getModelService().update(model, null);
                    } catch (Exception e) {
                        Log.warn("[{}][master] onDisconnect KevScript interpretation error:", instance.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getName(String id) {
        return this.ids.get(id);
    }

    /**
     * Replaces {{groupName}} by the current group instance name and {{nodeName}} by the given nodeName in parameter
     * @param kevs the KevScript to interpolate
     * @param nodeName the value to replace {{nodeName}} with
     * @return interpolated kevscript
     */
    private String kevsTpl(String kevs, String nodeName) {
        return kevs
                .replaceAll("\\{\\{nodeName\\}\\}", nodeName)
                .replaceAll("\\{\\{groupName\\}\\}", instance.getName());
    }
}
