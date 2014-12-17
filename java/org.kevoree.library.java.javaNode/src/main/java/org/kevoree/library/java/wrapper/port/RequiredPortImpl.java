package org.kevoree.library.java.wrapper.port;

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

    public void send(String payload, Callback callback) {
        if (!delegate.isEmpty()) {
            for (ChannelWrapper wrapper : delegate) {
                wrapper.call(callback, payload);
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
