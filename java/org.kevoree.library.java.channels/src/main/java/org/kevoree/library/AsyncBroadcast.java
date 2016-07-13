package org.kevoree.library;

import org.kevoree.annotation.*;
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
@ChannelType(version = 1, description = "<strong>This channel only works locally</strong>"+
"<br/>Sends messages <strong>asynchronously</strong> using a new Thread "+
"for each dispatch")
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
    public void dispatch(final String payload, final Callback callback) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                for (Port p : channelContext.getLocalPorts()) {
                    p.send(payload, callback);
                }
            }
        });
    }

}
