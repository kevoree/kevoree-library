package org.kevoree.library.client;

import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.protocol.Protocol;

/**
 *
 * Created by leiko on 1/11/17.
 */
class KevsHandler {

    static void process(Protocol.PushKevSMessage pMsg, CentralizedWSGroup instance) {
        instance.getModelService().submitScript(pMsg.getKevScript(), null);
    }
}
