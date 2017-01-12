package org.kevoree.library.client;

import org.kevoree.ContainerRoot;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelLoader;

/**
 *
 * Created by leiko on 1/11/17.
 */
class PushHandler {

    static void process(Protocol.PushMessage pMsg, CentralizedWSGroup instance) {
        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelLoader loader = factory.createJSONLoader();

        try {
            ContainerRoot model = (ContainerRoot) loader.loadModelFromString(pMsg.getModel()).get(0);
//            var id = shortid(10);
//            model.generated_KMF_ID = id;
//            server.modelId = id;
            instance.getModelService().update(model, null);
        } catch (Exception e) {
            Log.warn("[{}][client] erroneous model received (push ignored)", instance.getName());
        }
    }
}
