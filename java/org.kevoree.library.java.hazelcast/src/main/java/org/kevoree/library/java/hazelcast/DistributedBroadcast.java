package org.kevoree.library.java.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.Context;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 11:20
 */
@NodeType
public class DistributedBroadcast implements MessageListener {

    @KevoreeInject
    Context context;

    @Start
    public void start() {

    }

    @Stop
    public void stop() {

    }

    public static void main(String[] args) {
        Hazelcast.newHazelcastInstance().getTopic("");


        //
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(message);
    }
}
