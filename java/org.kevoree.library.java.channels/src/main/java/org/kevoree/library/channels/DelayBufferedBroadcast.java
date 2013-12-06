package org.kevoree.library.channels;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:05
 */
@ChannelType
@Library(name = "Java :: Channels")
public class DelayBufferedBroadcast implements ChannelDispatch, Runnable {

    class QueuedElement {
        Object payload;
        Callback callback;
    }

    @Param(optional = true)
    long delay = 5000;

    @KevoreeInject
    ChannelContext channelContext;

    private ScheduledExecutorService executor;
    private ArrayBlockingQueue<QueuedElement> queue = new ArrayBlockingQueue<QueuedElement>(30);

    public DelayBufferedBroadcast() {
    }

    @Start
    public void channelStart() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Stop
    public void channelStop() {
        queue.clear();
        executor.shutdownNow();
        executor = null;
    }


    @Override
    public void dispatch(Object payload, Callback callback) {
        try {
            QueuedElement e = new QueuedElement();
            e.callback = callback;
            e.payload = payload;
            queue.put(e);
        } catch (InterruptedException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    public void run() {
        while(!queue.isEmpty()) {
            QueuedElement e = queue.poll();
            for (Port p : channelContext.getLocalPorts()) {
                p.call(e.payload, e.callback);
            }
        }
    }

}
