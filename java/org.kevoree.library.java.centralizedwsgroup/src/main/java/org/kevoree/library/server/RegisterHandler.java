package org.kevoree.library.server;

import com.pusher.java_websocket.WebSocket;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.ConnIdentity;
import org.kevoree.library.util.GroupHelper;
import org.kevoree.library.util.ModelReducer;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.compare.ModelCompare;
import org.kevoree.pmodeling.api.json.JSONModelLoader;

import java.util.HashMap;

/**
 *
 * Created by leiko on 1/11/17.
 */
class RegisterHandler {

    static void process(WebSocket conn, HashMap<WebSocket, ConnIdentity> clients, Protocol.RegisterMessage pMsg,
                        CentralizedWSGroup instance) {

        ConnIdentity identity = clients.get(conn);
        identity.name = pMsg.getNodeName();
        Log.info("[{}][master] client \"{}\" connected (id={})", instance.getName(), identity.name, identity.id);

        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelLoader loader = factory.createJSONLoader();
        ContainerRoot model;

        try {
            model = (ContainerRoot) loader.loadModelFromString(pMsg.getModel()).get(0);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse register model from \""+identity.name+"\"");
        }

        ContainerRoot currentModel = instance.getModelService().getCurrentModel().getModel();
        Group group = (Group) currentModel.findByPath(instance.getContext().getPath());
        ContainerNode masterNode = GroupHelper.findMasterNode(group);
        Log.debug("[{}][master] reducing register model for master \"{}\" and client \"{}\"", instance.getName(),
                masterNode.getName(), identity.name);
        ContainerRoot registerModel = ModelReducer.reduce(model, masterNode.getName(), identity.name);
        ModelCompare compare = factory.createModelCompare();
        compare.merge(registerModel, currentModel).applyOn(registerModel);

        instance.getModelService().update(registerModel, null);
    }
}
