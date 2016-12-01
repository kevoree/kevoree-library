package org.kevoree.library.java.wrapper.port;

import org.kevoree.MBinding;
import org.kevoree.annotation.Output;
import org.kevoree.api.Callback;
import org.kevoree.api.CallbackResult;
import org.kevoree.api.Port;
import org.kevoree.api.helper.ReflectUtils;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.log.Log;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

public class RequiredPortImpl implements Port {

    private org.kevoree.Port port;
    private ComponentWrapper comp;
    private String portPath;
    private final Map<String, ChannelWrapper> channels = Collections.synchronizedMap(new HashMap<String, ChannelWrapper>());

    public RequiredPortImpl(Object targetObj, org.kevoree.Port port, ComponentWrapper componentWrapper) {
        this.port = port;
        this.comp = componentWrapper;
        this.portPath = port.path();

        Field field = ReflectUtils.findFieldWithAnnotation(port.getName(), targetObj.getClass(), Output.class);
        if (field != null) {
            try {
                boolean isAccessible = field.isAccessible();
                field.setAccessible(true);
                field.set(targetObj, this);
                field.setAccessible(isAccessible);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to set @Output field \"" + port.getName() + "\" implementation", e);
            }
        } else {
            throw new RuntimeException("Unable to find @Output port field \""+port.getName()+"\" in type " + targetObj.getClass());
        }
    }

    public int getConnectedBindingsSize() {
        return channels.size();
    }

    @Override
    public void send(String payload) {
        this.send(payload, null);
    }

    public void send(String payload, final Callback callback) {
        synchronized (channels) {
            if (!channels.isEmpty()) {
                Log.debug("{}.{} -> {}", comp.getModelElement().getName(), port.getName(), payload);
            } else {
                Log.debug("{}.{} -> {} (dropped)", comp.getModelElement().getName(), port.getName(), payload);
            }
            for (final ChannelWrapper channel : channels.values()) {
                channel.call(new Callback() {
                    @Override
                    public void onSuccess(CallbackResult result) {
                        result.setOriginChannelPath(channel.getContext().getChannelPath());
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Throwable exception) {
                        if (callback != null) {
                            callback.onError(exception);
                        } else {
                            if (exception != null) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }, payload);
            }
        }
    }

    public String getPath() {
        return portPath;
    }

    public void addChannelWrapper(MBinding binding, ChannelWrapper chan) {
        channels.put(binding.getHub().path()+"_"+binding.getPort().path(), chan);
    }

    public void removeChannelWrapper(MBinding binding) {
        channels.remove(binding.getHub().path()+"_"+binding.getPort().path());
    }
}
