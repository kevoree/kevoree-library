package org.kevoree.library.java.wrapper.port;

import org.kevoree.api.CallbackResult;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.api.Port;
import org.kevoree.api.Callback;
import org.kevoree.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/11/2013
 * Time: 11:26
 */

public class RequiredPortImpl implements Port {

    private String portPath;
    private List<ChannelWrapper> delegate = new ArrayList<ChannelWrapper>();

    public RequiredPortImpl(String portPath) {
        this.portPath = portPath;
    }

    public int getConnectedBindingsSize() {
        return delegate.size();
    }

    public void send(String payload, final Callback callback) {
        if (!delegate.isEmpty()) {
            for (final ChannelWrapper wrapper : delegate) {
                wrapper.call(new Callback() {
                    @Override
                    public void onSuccess(CallbackResult result) {
                        result.setOriginChannelPath(wrapper.getContext().getChannelPath());
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(Throwable exception) {
                        callback.onError(exception);
                    }
                }, payload);
            }
        } else {
            callback.onError(new Exception("Message lost, because port is not bind"));
            Log.warn("Message lost, because no binding found : {}", payload);
        }
    }

    public String getPath() {
        return portPath;
    }

    public List<ChannelWrapper> getDelegate() {
        return delegate;
    }
}
