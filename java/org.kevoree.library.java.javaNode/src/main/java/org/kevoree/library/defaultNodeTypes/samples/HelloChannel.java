package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelDispatch;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 16/11/2013
 * Time: 23:28
 */
@ChannelType
public class HelloChannel implements ChannelDispatch {

    @Start
    public void start() {
        System.out.println("Hello from channel");
    }

    @Stop
    public void stop() {
        System.out.println("Bye from channel");
    }

    @Override
    public void dispatch(Object payload, Callback callback) {
        System.out.println("dispatch="+payload);
    }

}
