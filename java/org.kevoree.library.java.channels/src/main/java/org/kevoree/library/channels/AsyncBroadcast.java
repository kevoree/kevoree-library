package org.kevoree.library.channels;

import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:05
 */
@ChannelType
public class AsyncBroadcast implements ChannelDispatch {

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

    @Override
    public void dispatch(final Object payload, final Callback callback) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println("Dispatch to "+channelContext.getLocalPorts().size());
                for (Port p : channelContext.getLocalPorts()) {
                    p.call(payload, callback);
                }
            }
        });
    }
}
