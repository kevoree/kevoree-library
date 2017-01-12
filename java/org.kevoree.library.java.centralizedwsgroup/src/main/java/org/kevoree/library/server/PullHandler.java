package org.kevoree.library.server;

import com.pusher.java_websocket.WebSocket;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.util.ConnIdentity;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.util.HashMap;

/**
 *
 * Created by leiko on 1/11/17.
 */
class PullHandler {

    static void process(WebSocket conn, HashMap<WebSocket, ConnIdentity> clients, CentralizedWSGroup instance) {
        ConnIdentity identity = clients.get(conn);
        Log.info("[{}][master] pull requested: {} (id={})", instance.getName(), identity.name, identity.id);

        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelSerializer serializer = factory.createJSONSerializer();
        conn.send(serializer.serialize(instance.getModelService().getCurrentModel().getModel()));
    }
}
