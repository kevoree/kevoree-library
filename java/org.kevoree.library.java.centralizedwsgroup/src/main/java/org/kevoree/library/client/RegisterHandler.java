package org.kevoree.library.client;

import com.pusher.java_websocket.WebSocket;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

/**
 *
 * Created by leiko on 1/11/17.
 */
class RegisterHandler {

    static void process(WebSocket conn, CentralizedWSGroup instance) {
        // XXX take extra caution of the possible side-effects of a register
        // XXX message received before the current node even end the first deployment
        // XXX which will inevitably result in unexpected model merging

        String nodeName = instance.getContext().getNodeName();
        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelSerializer serializer = factory.createJSONSerializer();
        String modelStr = serializer.serialize(instance.getModel());

        // sending register message
        Protocol.RegisterMessage rMsg = new Protocol.RegisterMessage(nodeName, modelStr);
        conn.send(rMsg.toRaw());
    }
}
