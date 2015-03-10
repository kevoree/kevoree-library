package org.kevoree.library;

import fr.braindead.wsmsgbroker.Response;
import fr.braindead.wsmsgbroker.WSMsgBrokerClient;
import fr.braindead.wsmsgbroker.callback.AnswerCallback;
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
        Set<String> inputPaths = getPortsPath(thisChan, "provided");
        Set<String> outputPaths = getPortsPath(thisChan, "required");

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
        Set<String> outputPaths = getPortsPath(thisChan, "required");

        // create a list of destination paths
        Set<String> destPaths = new HashSet<String>();
        // process remote paths in order to add _<chanName> to the paths (WsMsgBroker protocol)
        for (String remotePath : channelContext.getRemotePortPaths()) {
            // add processed remote path to dest
            destPaths.add(remotePath + "_" + context.getInstanceName());
        }
        // add local connected inputs to dest
        destPaths.addAll(getPortsPath(thisChan, "provided"));
        // create the array that will store the dest
        String[] dest = new String[destPaths.size()];
        // convert list to array
        destPaths.toArray(dest);

        for (final String outputPath : outputPaths) {
            WSMsgBrokerClient client = this.clients.get(outputPath);
            if (client != null) {
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
            } else {
                createInputClient(outputPath);
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
            public void onClose(int code, String reason) {
                Log.error("Connection closed by remote server for {}", id);
            }

            @Override
            public void onError(Exception e) {}
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
            public void onClose(int code, String reason) {
                Log.debug("Connection closed by remote server for {}", id);
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private Set<String> getPortsPath(Channel chan, String type) {
        Set<String> paths = new HashSet<String>();
        if (chan != null) {
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
        }
        return paths;
    }
}