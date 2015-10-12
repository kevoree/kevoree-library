package org.kevoree.library;

import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
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
@ChannelType(description = "<strong>This channel only works locally</strong>"+
"<br/>Sends messages <strong>synchronously</strong> for each dispatch")
public class SyncBroadcast implements ChannelDispatch {

    @KevoreeInject
    ChannelContext channelContext;

    @Override
    public void dispatch(String payload, final Callback callback) {
        for (Port p : channelContext.getLocalPorts()) {
            p.send(payload, callback);
        }
    }
}
