package org.kevoree.library.wrapper.port;

import org.kevoree.Channel;
import org.kevoree.Port;
import org.kevoree.api.Callback;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by leiko on 3/3/17.
 */
public class RemoteInputPort implements org.kevoree.api.Port {

    private Port port;

    public RemoteInputPort(Port port) {
        this.port = port;
    }

    @Override
    public void send(String payload) {
        // noop: makes no sense to call .send(...) on a remote input
    }

    @Override
    public void send(String payload, Callback callback) {
        // noop: makes no sense to call .send(...) on a remote input
    }

    @Override
    public String getPath() {
        return this.port.path();
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        this.port.getBindings().forEach(binding -> {
            if (binding.getHub() != null) {
                channels.add(binding.getHub());
            }
        });
        return channels;
    }
}
