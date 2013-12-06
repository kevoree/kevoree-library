package org.kevoree.library.channels;

import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Library;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:05
 */
@ChannelType
@Library(name = "Java :: Channels")
public class SyncBroadcast implements ChannelDispatch {

    @KevoreeInject
    ChannelContext channelContext;

    @Override
    public void dispatch(Object payload, Callback callback) {
        for (Port p : channelContext.getLocalPorts()) {
            p.call(payload, callback);
        }
    }
}
