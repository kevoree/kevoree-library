package org.kevoree.library.server;

import com.pusher.java_websocket.WebSocket;
import org.kevoree.ContainerRoot;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.ConnIdentity;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelLoader;

import java.util.HashMap;

/**
 *
 * Created by leiko on 1/11/17.
 */
class PushHandler {

    static void process(WebSocket conn, HashMap<WebSocket, ConnIdentity> clients, Protocol.PushMessage pMsg,
                        CentralizedWSGroup instance) {
        ConnIdentity identity = clients.get(conn);
        if (identity.name == null) {
            // anonymous push
            Log.info("[{}][master] push issued by {}", instance.getName(), conn.getRemoteSocketAddress());
            KevoreeFactory factory = new DefaultKevoreeFactory();
            JSONModelLoader loader = factory.createJSONLoader();

            try {
                ContainerRoot model = (ContainerRoot) loader.loadModelFromString(pMsg.getModel()).get(0);
//            var id = shortid(10);
//            model.generated_KMF_ID = id;
//            server.modelId = id;
                instance.getModelService().update(model, null);
            } catch (Exception e) {
                Log.warn("[{}][master] erroneous model received (push ignored)", instance.getName());
            }
        } else {
            // registered node push
            // XXX should not happen
        }

//        var nodeName = client2name[client.id];
//        if (nodeName) {
//            logger.info('push issued by: ' + nodeName);
//        } else {
//            var origin = 'anonymous';
//            if (client.upgradeReq.headers.origin) {
//                origin = client.upgradeReq.headers.origin;
//            }
//            logger.info('push issued by ' + origin);
//        }
    }
}
