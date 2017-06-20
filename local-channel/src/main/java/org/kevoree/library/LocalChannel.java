package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.annotation.ChannelType;
import org.kevoree.api.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ChannelType(version = 1, description = "Kevoree channel that only sends messages to components in the same node")
public class LocalChannel implements ChannelDispatch {

    @Param(optional = false)
    private int delay = 0;

    @KevoreeInject
    private ChannelContext context = null;

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
        executor.schedule(() -> {
            for (Port p : context.getLocalInputs()) {
                p.send(payload);
            }
        }, this.delay, TimeUnit.MILLISECONDS);
    }
}
