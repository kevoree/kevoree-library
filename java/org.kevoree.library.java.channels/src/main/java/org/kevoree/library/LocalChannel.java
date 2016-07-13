package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ChannelType(version = 1, description = "Kevoree channel that only sends messages to components in the same node")
public class LocalChannel implements ChannelDispatch {

    @Param(optional = false, defaultValue = "0")
    private int delay = 0;

    @KevoreeInject
    ChannelContext channelContext;

    private ScheduledExecutorService executor = null;

    @Start
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Stop
    public void stop() {
        executor.shutdownNow();
    }

    @Override
    public void dispatch(final String payload, final Callback callback) {
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                for (Port p : channelContext.getLocalPorts()) {
                    p.send(payload, callback);
                }
            }
        }, this.delay, TimeUnit.MILLISECONDS);
    }
}
