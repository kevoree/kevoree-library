package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

import java.util.concurrent.*;

/**
 * Waits until the buffer is full to send the messages.
 * The size of the buffer can be customized with the bufferSize property.
 */
@ChannelType(description = "<strong>This channel only works locally</strong>" +
        "<br/>When this channel receives a message it will add it in a queue and will send it when the queue size " +
        "reaches the given <strong>bufferSize</strong> dictionary attribute value" +
        "<br/><br/><em>NB: when the channel stops, the queue is cleared (which means that restarting this channel will lost all queued messages)</em>")
public class SizeBufferedBroadcast implements ChannelDispatch, Runnable {

    class QueuedElement {
        String payload;
        Callback callback;
    }

    @Param(optional = true)
    int bufferSize = 5;

    @KevoreeInject
    ChannelContext channelContext;

    private ExecutorService executor;
    private ArrayBlockingQueue<QueuedElement> queue = new ArrayBlockingQueue<QueuedElement>(bufferSize);

    @Start
    public void channelStart() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Stop
    public void channelStop() {
        queue.clear();
        executor.shutdownNow();
        executor = null;
    }


    @Override
    public void dispatch(final String payload, final Callback callback) {
        try {
            QueuedElement e = new QueuedElement();
            e.callback = callback;
            e.payload = payload;
            queue.put(e);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        if (queue.size() >= bufferSize) {
            executor.submit(this);
        }

    }


    public void run() {
        while (!queue.isEmpty()) {
            QueuedElement e = queue.poll();
            for (Port p : channelContext.getLocalPorts()) {
                p.send(e.payload, e.callback);
            }
        }
    }

}
