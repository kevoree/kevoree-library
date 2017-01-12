package org.kevoree.library.client;

import com.pusher.java_websocket.client.WebSocketClient;
import com.pusher.java_websocket.handshake.ServerHandshake;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.library.CentralizedWSGroup;
import org.kevoree.library.FragmentFacade;
import org.kevoree.library.KevoreeParamException;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.util.GroupHelper;
import org.kevoree.log.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by leiko on 1/10/17.
 */
public class Client implements FragmentFacade {

    private static final Pattern MASTER_NET = Pattern.compile("^([a-z0-9A-Z]+)\\.([a-z0-9A-Z]+)$");
    private CentralizedWSGroup instance;
    private WebSocketClient wsClient;

    public Client(CentralizedWSGroup instance) {
        this.instance = instance;
    }

    @Override
    public void create() {
        this.wsClient = new WebSocketClient(getUri(instance)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.info("[{}][client] connected to {}", instance.getName(), uri);
                RegisterHandler.process(this, instance);
            }

            @Override
            public void onMessage(String message) {
                Log.debug("onMessage: {}");
                Protocol.Message pMsg = Protocol.parse(message);
                if (pMsg != null) {
                    switch (pMsg.getType()) {
                        case Protocol.PUSH_TYPE:
                            PushHandler.process((Protocol.PushMessage) pMsg, instance);
                            break;

                        case Protocol.KEVS_TYPE:
                            KevsHandler.process((Protocol.PushKevSMessage) pMsg, instance);
                            break;

                        default:
                            Log.warn("[{}][client] protocol message type \"{}\" not handled yet.", instance.getName(),
                                    Protocol.getTypeName(pMsg.getType()));
                            break;
                    }
                } else {
                    Log.warn("[{}][client] unable to parse message", instance.getName());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.debug("onClose (code={},reason={})", code, reason);
                if (code != 1000) {
                    Log.warn("connection lost with {} (code={},reason={}) (TODO retry loop)", uri, code, reason);
                } else {
                    Log.info("connection closed with {} (code=1000)", uri);
                }
            }

            @Override
            public void onError(Exception ex) {
                Log.warn("onError");
                ex.printStackTrace();
            }
        };
        this.wsClient.connect();
    }

    private URI getUri(CentralizedWSGroup instance) {
        String uri;
        if (instance.getPort() == 443) {
            uri = "wss://";
        } else {
            uri = "ws://";
        }
        Matcher masterNetMatcher = MASTER_NET.matcher(instance.getMasterNet());
        if (masterNetMatcher.matches()) {
            String masterNetName = masterNetMatcher.group(1);
            String masterNetValueName = masterNetMatcher.group(2);
            ContainerRoot currentModel = instance.getModel();
            Group group = (Group) currentModel.findByPath(instance.getContext().getPath());
            ContainerNode masterNode = GroupHelper.findMasterNode(group);
            if (masterNode != null) {
                HashMap<String, HashMap<String, String>> nets = GroupHelper.findMasterNets(group, masterNode);
                HashMap<String, String> masterNetValues = nets.get(masterNetName);
                if (masterNetValues != null) {
                    String networkValue = masterNetValues.get(masterNetValueName);
                    if (networkValue != null) {
                        return URI.create(uri + networkValue + ":" + instance.getPort());
                    } else {
                        throw new KevoreeParamException("Unable to find network value name \""+masterNetValueName+"\" for master node \""+masterNode.getName()+"\"");
                    }
                } else {
                    throw new Error("Unable to find network \""+masterNetName+"\" for master node \""+masterNode.getName()+"\"");
                }
            } else {
                throw new KevoreeParamException("No master node found. Did you at least set one \"isMaster\" to \"true\"?");
            }
        } else {
            throw new KevoreeParamException(instance.getModelService().getNodeName(), "masterNet",
                    "must comply with /^([a-z0-9A-Z]+)\\\\.([a-z0-9A-Z]+)$/");
        }
    }

    @Override
    public void close() {
        if (this.wsClient != null) {
            this.wsClient.close();
        }
    }
}
