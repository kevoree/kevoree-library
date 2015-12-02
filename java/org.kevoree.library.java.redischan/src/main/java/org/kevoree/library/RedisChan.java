package org.kevoree.library;

import com.google.gson.Gson;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import redis.clients.jedis.Jedis;

import org.kevoree.Channel;
import org.kevoree.ContainerRoot;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mleduc on 02/12/15.
 */
@ChannelType
public class RedisChan implements ChannelDispatch {
    @Param(defaultValue = "localhost", optional = false)
    private String host;

    @Param(defaultValue = "6379", optional = false)
    private int port;

    @Param(optional = false)
    private String prefix;

    private Jedis jedis;

    @KevoreeInject
    Context context;

    @KevoreeInject
    ChannelContext channelContext;

    @KevoreeInject
    private ModelService modelService;
    private ExecutorService executor;
    private List<RedisSubscriber> collect;

    @Start
    public void start() {
        jedis = new Jedis(host, port);

        final Set<String> ip = inputPaths();
        executor = Executors.newFixedThreadPool(ip.size());
        collect = ip.stream().map(x -> getSubscriber(x)).collect(Collectors.toList());
        collect.forEach(x -> executor.execute(() -> new Jedis(host, port).subscribe(x, prefix + Md5Utils.md5(x.getPath()))));

    }

    private RedisSubscriber getSubscriber(String x) {
        final Port port = channelContext.getLocalPorts().stream().filter(y -> y.getPath().equals(x)).findFirst().orElseGet(null);
        return new RedisSubscriber(port);
    }

    @Stop
    public void stop() {
        jedis.close();
        executor.shutdown();
        collect.forEach(x -> x.unsubscribe());

    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    private Set<String> inputPaths() {
        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }
        Channel thisChan = (Channel) model.findByPath(context.getPath());
        return Util.getInputPath(thisChan, context.getNodeName());
    }

    @Override
    public void dispatch(String payload, Callback callback) {
        final Gson payloadDocument = new Gson();

        final Stream<String> localInputPortStream = inputPaths().stream();
        final Stream<String> remoteInputPortStream = channelContext.getRemotePortPaths().stream();
        final Stream<String> inputPortStream = Stream.concat(localInputPortStream, remoteInputPortStream);
        inputPortStream.forEach((x) -> jedis.publish(this.prefix + Md5Utils.md5(x), payload));


    }


}
