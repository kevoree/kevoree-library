package org.kevoree.library;

import org.kevoree.Channel;
import org.kevoree.ComponentInstance;
import org.kevoree.api.Callback;
import org.kevoree.api.ModelService;
import org.kevoree.library.defaultNodeTypes.ModelRegistry;
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelWrapper;
import org.kevoree.library.defaultNodeTypes.wrapper.ComponentWrapper;
import org.kevoree.library.defaultNodeTypes.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.defaultNodeTypes.wrapper.port.RequiredPortImpl;
import org.kevoree.log.Log;

import java.util.concurrent.Callable;

/**
 * Created by duke on 8/26/14.
 */
public class ExternalMessageInjection implements Callable<String> {

    private String payload;

    private String path;

    private ModelRegistry registry;

    private ModelService service;

    public ExternalMessageInjection(String p, String path, ModelRegistry reg, ModelService service) {
        this.payload = p;
        this.path = path;
        this.registry = reg;
        this.service = service;
    }

    public static final String channelPath = "/channels/";

    public static final String componentPath = "/components/";


    @Override
    public String call() throws Exception {
        try {
            if (path.startsWith(channelPath)) {
                String id = path.substring(channelPath.length());
                Channel channel = this.service.getCurrentModel().getModel().findHubsByID(id);
                if (channel != null) {
                    ChannelWrapper obj = (ChannelWrapper) this.registry.lookup(channel);
                    if (obj != null) {
                        obj.call(new Callback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                //TODO collect result here
                            }

                            @Override
                            public void onError(Throwable exception) {

                            }
                        }, payload);
                    } else {
                        return "channel " + id + " not found on this platform";
                    }
                }
            }
            if (path.startsWith(componentPath)) {
                String id = path.substring(componentPath.length());
                if (!id.contains("/")) {
                    return "malformed url /components/<componentID>/<portID> expected, actually " + path;
                }
                String[] paths = id.split("/");
                if (paths.length != 2) {
                    return "malformed url /components/<componentID>/<portID> expected, actually " + path + ": " + paths.length;
                }
                ComponentInstance component = this.service.getCurrentModel().getModel().findNodesByID(this.service.getNodeName()).findComponentsByID(paths[0]);
                if (component != null) {
                    ComponentWrapper obj = (ComponentWrapper) this.registry.lookup(component);
                    if (obj != null) {
                        ProvidedPortImpl pport = obj.getProvidedPorts().get(paths[1]);
                        if (pport != null) {
                            pport.call(payload, new Callback() {
                                @Override
                                public void onSuccess(Object result) {

                                }

                                @Override
                                public void onError(Throwable exception) {

                                }
                            });
                        }
                        RequiredPortImpl rport = obj.getRequiredPorts().get(paths[1]);
                        if (rport != null) {
                            rport.call(payload, new Callback() {
                                @Override
                                public void onSuccess(Object result) {

                                }

                                @Override
                                public void onError(Throwable exception) {

                                }
                            });
                        }
                    } else {
                        return "component " + id + " not found on this platform";
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Error during processing external call ", e);
        }
        //TODO block if result is necessary
        return "ack";
    }
}
