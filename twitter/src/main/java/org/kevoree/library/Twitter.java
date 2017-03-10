package org.kevoree.library;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.kevoree.annotation.*;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 *
 * Created by leiko on 16/11/15.
 */
@ComponentType(version = 1, description = "Outputs tweets on the <strong>out</strong> port (JSON-encoded strings) based on the "+
"given <strong>followingIDs</strong> and <strong>trackTerms</strong>."+
"<br/>"+
"In order to access the Twitter API, you have to provide valid auths for <strong>consumerKey</strong>, "+
"<strong>consumerSecret</strong>, <strong>token</strong> and <strong>secret</strong>")
public class Twitter {

    private Client client;
    private BlockingQueue<String> queue;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Param(optional = false)
    private String consumerKey;

    @Param(optional = false)
    private String consumerSecret;

    @Param(optional = false)
    private String token;

    @Param(optional = false)
    private String secret;

    @Param
    private String trackTerms;

    @Param
    private String followingIDs;

    @Output
    private Port out;

    @Start
    public void start() {
        queue = new LinkedBlockingQueue<>(100000);
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(1000);

        // Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth)
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // set up some track terms
        if (trackTerms != null && !trackTerms.isEmpty()) {
            hosebirdEndpoint.trackTerms(Lists.newArrayList(trackTerms.split(" ")));
        }
        // set up some followings
        if (followingIDs != null && !followingIDs.isEmpty()) {
            Set<Long> followings = new HashSet<>();
            for (String id: followingIDs.split(" ")) {
                followings.add(Long.parseLong(id));
            }
            hosebirdEndpoint.followings(Lists.newArrayList(followings));
        }

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("twitter-client")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(queue))
                .eventMessageQueue(eventQueue);

        client = builder.build();
        // Attempts to establish a connection.
        client.connect();

        executor.submit(() -> {
            while (client != null && !client.isDone()) {
                try {
                    String msg = queue.poll(5000, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        out.send(msg, null);
                    }
                } catch (InterruptedException e) {
                    Log.warn("Twitter messages blocking queue interrupted while waiting.");
                }
            }
        });
    }

    @Stop
    public void stop() {
        client.stop(10000);
    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }
}
