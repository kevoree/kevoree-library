package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.api.Port;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 *
 * Created by mleduc on 02/12/15.
 */
@ChannelType(version = 1)
public class RedisChan implements ChannelDispatch {

    @Param(defaultValue = "localhost", optional = false)
    private String host;

    @Param(defaultValue = "6379", optional = false)
    private int port;

    @Param(optional = false)
    private String prefix;

    @KevoreeInject
    private ChannelContext context;

    private ExecutorService executor;
    private Jedis jedis;
    private List<RedisSubscriber> collect;

    @Start
    public void start() {
        jedis = new Jedis(host, port);

        Set<Port> inputs = context.getInputs();
        executor = Executors.newFixedThreadPool(inputs.size());
        collect = inputs.stream().map(RedisSubscriber::new).collect(Collectors.toList());
        collect.forEach(sub -> executor.execute(() -> new Jedis(host, port).subscribe(sub, prefix + Md5Utils.md5(sub.getPath()))));

    }

    @Stop
    public void stop() {
        jedis.close();
        executor.shutdown();
        collect.forEach(sub -> sub.unsubscribe());

    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    @Override
    public void dispatch(String payload, Callback callback) {
        context.getInputs().forEach(p -> jedis.publish(this.prefix + Md5Utils.md5(p.getPath()), payload));
    }


}
