package org.kevoree.library;

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

@ChannelType(version = 1, description = "<strong>This channel only works locally</strong>"+
"<br/>Sends message synchronously <strong>to only one</strong> of the input. "+
"The input port that will receive the message is selected randomly for each "+
"messages")
public class LoadBalancer implements ChannelDispatch {

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

    @Override
    public void dispatch(final String payload, final Callback callback) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                List<Port> ports = channelContext.getLocalPorts();
                Port selected = ports.get(random.nextInt(ports.size()));
                selected.send(payload, callback);
            }
        });
    }
}
