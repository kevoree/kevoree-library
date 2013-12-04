package org.kevoree.library.java.hazelcast;

import com.hazelcast.core.*;
import org.kevoree.annotation.ChannelType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Context;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 11:20
 */
@ChannelType
public class DistributedBroadcast implements MessageListener, ChannelDispatch {

    @KevoreeInject
    Context context;

    private HazelcastInstance localHazelCast = null;
    private ITopic topic = null;

    @Start
    public void start() {
        localHazelCast = Hazelcast.newHazelcastInstance();
        topic = localHazelCast.getTopic(context.getInstanceName());

    }

    @Stop
    public void stop() {
        localHazelCast.shutdown();
    }

    @Override
    public void onMessage(Message message) {
        if (!message.getPublishingMember().localMember()) {
            message.getMessageObject();
        }
    }

    //TODO periodic cleanup for TTL
    private HashMap<UUID, Callback> cache = new HashMap<UUID, Callback>();

    @Override
    public void dispatch(Object payload, Callback callback) {


    }

    class InternalCall {
        UUID id = UUID.randomUUID();
        public InternalCall(Object obj){

        }
    }

}
