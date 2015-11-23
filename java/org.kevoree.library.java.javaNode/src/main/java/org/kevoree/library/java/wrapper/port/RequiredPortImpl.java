package org.kevoree.library.java.wrapper.port;

import org.kevoree.MBinding;
import org.kevoree.api.CallbackResult;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.api.Port;
import org.kevoree.api.Callback;
import org.kevoree.log.Log;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

public class RequiredPortImpl implements Port {

    private String portPath;
    private final Map<String, ChannelWrapper> delegate = Collections.synchronizedMap(new HashMap<String, ChannelWrapper>());

    public RequiredPortImpl(String portPath) {
        this.portPath = portPath;
    }

    public int getConnectedBindingsSize() {
        return delegate.size();
    }

    @Override
    public void send(String payload) {
        this.send(payload, null);
    }

    public void send(String payload, final Callback callback) {
        synchronized (delegate) {
            if (!delegate.isEmpty()) {
                for (final ChannelWrapper wrapper : delegate.values()) {
                    wrapper.call(new Callback() {
                        @Override
                        public void onSuccess(CallbackResult result) {
                            result.setOriginChannelPath(wrapper.getContext().getChannelPath());
                            if(callback != null) {
                                callback.onSuccess(result);
                            }
                        }

                        @Override
                        public void onError(Throwable exception) {
                            if(callback != null) {
                                callback.onError(exception);
                            } else {
                                if(exception != null) {
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }, payload);
                }
            } else {
                callback.onError(new Exception("Message lost because the port is not connected to any channel"));
            }
        }
    }

    public String getPath() {
        return portPath;
    }

    public void addChannelWrapper(MBinding binding, ChannelWrapper chan) {
        delegate.put(binding.getHub().path()+"_"+binding.getPort().path(), chan);
    }

    public void removeChannelWrapper(MBinding binding) {
        delegate.remove(binding.getHub().path()+"_"+binding.getPort().path());
    }
}
