package org.kevoree.library.channels;

import com.rits.cloning.Cloner;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by duke on 05/12/2013.
 */

@ChannelType
public class LoadBalancer implements ChannelDispatch {

    @Param(defaultValue = "false")
    boolean clone;

    @KevoreeInject
    ChannelContext channelContext;

    ExecutorService executor = null;

    @Start
    public void start() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Stop
    public void stop() {
        executor.shutdownNow();
    }

    private Random random = new Random();
    private Cloner cloner = new Cloner();

    @Override
    public void dispatch(final Object payload, final Callback callback) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                List<Port> ports = channelContext.getLocalPorts();
                Port selected = ports.get(random.nextInt(ports.size()));
                if (clone) {
                    selected.call(cloner.deepClone(payload), callback);
                } else {
                    selected.call(payload, callback);
                }
            }
        });
    }
}
