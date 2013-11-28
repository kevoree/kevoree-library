package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 16/11/2013
 * Time: 23:28
 */
@ChannelType
public class HelloChannel implements ChannelDispatch {

    @KevoreeInject
    ChannelContext channelContext;

    @Start
    public void start() {
        System.out.println("Hello from channel");
        System.out.println(channelContext.getLocalPorts().size());

    }

    @Stop
    public void stop() {
        System.out.println("Bye from channel");
    }

    @Override
    public void dispatch(Object payload, Callback callback) {
        System.out.println("dispatch=" + payload);
        //Direct call
        for (Port p : channelContext.getLocalPorts()) {
            p.call(payload, callback);
        }
    }

}
