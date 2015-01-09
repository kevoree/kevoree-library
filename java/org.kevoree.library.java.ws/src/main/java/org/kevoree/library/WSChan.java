package org.kevoree.library;

import fr.braindead.wsmsgbroker.Response;
import fr.braindead.wsmsgbroker.WSMsgBrokerClient;
import fr.braindead.wsmsgbroker.callback.AnswerCallback;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.kevoree.*;
import org.kevoree.annotation.*;
import org.kevoree.annotation.ChannelType;
import org.kevoree.api.*;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.util.*;

@ChannelType
public class WSChan implements ChannelDispatch {

    @KevoreeInject
    Context context;
    @KevoreeInject
    ChannelContext channelContext;
    @KevoreeInject
    ModelService modelService;

    @Param
    private String path;
    @Param
    private int port;
    @Param
    private String host;

    private Map<String, WSMsgBrokerClient> clients;

    @Start
    public void start() {
        clients = new HashMap<String, WSMsgBrokerClient>();
        if (path == null) {
            path = "";
        }

        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }
        Channel thisChan = (Channel) model.findByPath(context.getPath());
        List<String> inputPaths = getPortsPath(thisChan, "provided");
        List<String> outputPaths = getPortsPath(thisChan, "required");

        for (String path : inputPaths) {
            // create input WSMsgBroker clients
            createInputClient(path);
        }

        for (String path : outputPaths) {
            // create output WSMsgBroker clients
            createOutputClient(path);
        }
    }

    @Stop
    public void stop() {
        if (this.clients != null) {
            for (WSMsgBrokerClient client : clients.values()) {
                if (client != null) {
                    client.close();
                }
            }
            this.clients = null;
        }
    }

    @Update
    public void update() {
        stop();
        start();
    }

    @Override
    public void dispatch(String o, final Callback callback) {
        ContainerRoot model = modelService.getCurrentModel().getModel();
        Channel thisChan = (Channel) model.findByPath(context.getPath());
        List<String> outputPaths = getPortsPath(thisChan, "required");

        String[] dest = new String[channelContext.getRemotePortPaths().size()];
        channelContext.getRemotePortPaths().toArray(dest);

        for (final String outputPath : outputPaths) {
            WSMsgBrokerClient client = this.clients.get(outputPath);
            if (client != null) {
                try {
                    if (callback != null) {
                        client.send(o, dest, new AnswerCallback() {
                            @Override
                            public void execute(String from, Object o) {
                                CallbackResult result = new CallbackResult();
                                result.setPayload(o.toString());
                                result.setOriginChannelPath(context.getPath());
                                result.setOriginPortPath(outputPath);
                                callback.onSuccess(result);
                            }
                        });
                    } else {
                        client.send(o, dest);
                    }
                } catch (WebsocketNotConnectedException e) {
                    Log.warn("Unable to send message, no connection established for {}", outputPath);
                }
            }
        }
    }

    private void createInputClient(final String id) {
        this.clients.put(id, new WSMsgBrokerClient(id, host, port, path, true) {
            @Override
            public void onUnregistered(String s) {
                Log.info("{} unregistered from remote server", id);
            }

            @Override
            public void onRegistered(String s) {
                Log.info("{} registered on remote server", id);
            }

            @Override
            public void onMessage(Object o, final Response response) {
                Callback cb = null;

                if (response != null) {
                    cb = new Callback() {
                        @Override
                        public void onSuccess(CallbackResult o) {
                            if (o != null) {
                                response.send(o);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            response.send(throwable);
                        }
                    };
                }

                List<Port> ports = channelContext.getLocalPorts();
                for (Port p : ports) {
                    p.send(o.toString(), cb);
                }
            }

            @Override
            public void onError(Exception e) {
//                Log.error("Something went wrong with the connection of {} (reason: {})", id, e.getMessage());
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.error("Connection closed by remote server for {}", id);
            }
        });
    }

    private void createOutputClient(final String id) {
        this.clients.put(id, new WSMsgBrokerClient(id, host, port, path, true) {
            @Override
            public void onUnregistered(String s) {
                Log.debug("{} unregistered from remote server", id);
            }

            @Override
            public void onRegistered(String s) {
                Log.debug("{} registered on remote server", id);
            }

            @Override
            public void onMessage(Object o, final Response response) {
            }

            @Override
            public void onError(Exception e) {
                Log.debug("Something went wrong with the connection of {} (reason: {})", id, e.getMessage());
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.debug("Connection closed by remote server for {}", id);
            }
        });
    }

    private List<String> getPortsPath(Channel chan, String type) {
        List<String> paths = new ArrayList<String>();
        for (MBinding binding : chan.getBindings()) {
            if (binding.getPort() != null
                    && binding.getPort().getRefInParent() != null
                    && binding.getPort().getRefInParent().equals(type)) {
                ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                if (node.getName().equals(context.getNodeName())) {
                    paths.add(binding.getPort().path()+"_"+context.getInstanceName());
                }
            }
        }
        return paths;
    }
}