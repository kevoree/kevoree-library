package org.kevoree.library.channels;

import com.rits.cloning.Cloner;
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
@ChannelType
public class SizeBufferedBroadcast implements ChannelDispatch, Runnable {

    class QueuedElement {
        Object payload;
        Callback callback;
    }

    @Param(defaultValue = "false")
    boolean clone;

    @Param(optional = true)
    int bufferSize = 5;

    @KevoreeInject
    ChannelContext channelContext;

    private ExecutorService executor;
    private ArrayBlockingQueue<QueuedElement> queue = new ArrayBlockingQueue<QueuedElement>(bufferSize + 3);

    public SizeBufferedBroadcast() {
    }

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

    private Cloner cloner=new Cloner();

    @Override
    public void dispatch(final Object payload,final Callback callback) {
        try {
            QueuedElement e = new QueuedElement();
            e.callback = callback;
            e.payload = payload;
            queue.put(e);
        } catch (InterruptedException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if(queue.size() >= bufferSize) {
            executor.submit(this);
        }

    }


    public void run() {
        while(!queue.isEmpty()) {
            QueuedElement e = queue.poll();
            for (Port p : channelContext.getLocalPorts()) {
                if(clone){
                    p.call(cloner.deepClone(e.payload),e.callback);
                } else {
                    p.call(e.payload,e.callback);
                }
            }
        }
    }

}
