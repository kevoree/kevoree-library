package org.kevoree.library;

import com.rits.cloning.Cloner;
import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Param;
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
public class SyncBroadcast implements ChannelDispatch {

    @Param(defaultValue = "false")
    boolean clone;

    @KevoreeInject
    ChannelContext channelContext;

    private Cloner cloner=new Cloner();

    @Override
    public void dispatch(final Object payload, final Callback callback) {
        for (Port p : channelContext.getLocalPorts()) {
            if(clone){
                p.call(cloner.deepClone(payload),callback);
            } else {
                p.call(payload,callback);
            }
        }
    }
}
