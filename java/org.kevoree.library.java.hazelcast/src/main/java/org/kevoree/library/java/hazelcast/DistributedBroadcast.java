package org.kevoree.library.java.hazelcast;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.library.java.hazelcast.message.Request;
import org.kevoree.library.java.hazelcast.message.Response;
import org.kevoree.log.Log;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 11:20
 */
@ChannelType
@Library(name = "Java :: Channels")
public class DistributedBroadcast implements MessageListener, ChannelDispatch {

    @KevoreeInject
    Context context;

    @KevoreeInject
    ChannelContext channelContext;

    private HazelcastInstance localHazelCast = null;
    private ITopic topic = null;

    @Start
    public void start() {
        Config config = new Config();
        config.setClassLoader(DistributedBroadcast.class.getClassLoader());
        localHazelCast = Hazelcast.newHazelcastInstance(config);
        topic = localHazelCast.getTopic(context.getInstanceName());
        topic.addMessageListener(this);
    }

    @Stop
    public void stop() {
        localHazelCast.shutdown();
    }

    @Override
    public void onMessage(Message message) {
        if (!message.getPublishingMember().localMember()) {
            Object payload = message.getMessageObject();
            if (payload instanceof Request) {
                final Request internalCall = (Request) message.getMessageObject();
                for (Port p : channelContext.getLocalPorts()) {
                    p.call(((Request) payload).getPayload(),new Callback() {
                        @Override
                        public void onSuccess(Object result) {
                            Response response = new Response(internalCall.getId(), result);
                            topic.publish(response);
                        }

                        @Override
                        public void onError(Throwable exception) {
                            Response response = new Response(internalCall.getId(), exception);
                            topic.publish(response);
                        }
                    });
                }
            }
            if (payload instanceof Response) {
                Response interalResponse = (Response) message.getMessageObject();
                Callback correspondingCall = cache.get(interalResponse.getId());
                if (correspondingCall != null) {
                    if (interalResponse.getPayload() instanceof Throwable) {
                        correspondingCall.onError((Throwable) interalResponse.getPayload());
                    } else {
                        try {
                            Type t = correspondingCall.getClass().getGenericInterfaces()[0];
                            if (t instanceof ParameterizedType) {
                                ((Class) ((ParameterizedType) t).getActualTypeArguments()[0]).cast(interalResponse.getPayload());
                            }
                            correspondingCall.onSuccess(interalResponse.getPayload());
                        } catch (Exception e) {
                            if (interalResponse.getPayload() != null) {
                                correspondingCall.onError(new Exception("Bad Callback parameter " + interalResponse.getPayload().getClass().getName()));
                            } else {
                                correspondingCall.onError(new Exception("Bad Callback parameter for null"));
                            }
                        }
                    }
                }
            }
        }
    }

    //TODO periodic cleanup for TTL
    private ConcurrentLinkedHashMap<UUID, Callback> cache = new ConcurrentLinkedHashMap.Builder<UUID, Callback>().maximumWeightedCapacity(1000).build();


    //= new ConcurrentHashMap<UUID, Callback>();

    @Override
    public void dispatch(final Object payload,final Callback callback) {
        Request internalCall = new Request(payload);
        if (callback != null) {
            cache.put(internalCall.getId(), callback);
        }
        topic.publish(internalCall);
        for (Port p : channelContext.getLocalPorts()) {
            p.call(payload,callback);
        }
    }

}
