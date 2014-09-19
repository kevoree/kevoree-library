package org.kevoree.library;

import com.rits.cloning.Cloner;
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
@ChannelType
public class AsyncBroadcast implements ChannelDispatch {

    @Param(defaultValue = "false")
    boolean clone;

    @KevoreeInject
    ChannelContext channelContext;

    ExecutorService executor = null;
    private Cloner cloner=new Cloner();

    @Start
    public void start() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Stop
    public void stop() {
        executor.shutdownNow();
    }

    @Override
    public void dispatch(final Object payload,final Callback callback) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                for (Port p : channelContext.getLocalPorts()) {
                    if(clone){
                        p.call(cloner.deepClone(payload),callback);
                    } else {
                        p.call(payload,callback);
                    }
                }
            }
        });
    }

}
