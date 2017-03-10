package org.kevoree.library;

import com.restfb.*;
import com.restfb.types.Post;
import org.kevoree.api.Port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mleduc on 16/11/15.
 */
public class FacebookClientThread extends Thread {

    private final String ressourceId;
    private final Port stream;
    private final DefaultFacebookClient facebookClient;
    public static final JsonMapper JSON_MAPPER = new DefaultJsonMapper();
    private long latestTime = System.currentTimeMillis();

    public FacebookClientThread(final String accessToken, final String ressourceId, final Port stream) {
        this.ressourceId = ressourceId;
        this.stream = stream;
        this.facebookClient = new DefaultFacebookClient(accessToken, Version.VERSION_2_5);
    }

    @Override
    public void run() {
        final List<Post> sortedPosts = getPosts(ressourceId, facebookClient, getParamSince(latestTime));
        send(sortedPosts);
        if(!sortedPosts.isEmpty()) {
            latestTime = sortedPosts.get(sortedPosts.size() - 1).getCreatedTime().getTime();
        }
    }

    private Parameter getParamSince(final long time) {
        return Parameter.with("since", time / 1000);
    }

    private List<Post> getPosts(final String page, final FacebookClient facebookClient, final Parameter... params) {
        final List<Post> sortedPosts;
        final Connection<Post> posts = query(page, facebookClient, params);
        if (posts.getData().isEmpty()) {
            sortedPosts = Collections.emptyList();
        } else {
            sortedPosts = sortStreamByCreationTime(listToStream(posts)).collect(Collectors.toList());
        }
        return sortedPosts;
    }

    private void send(final List<Post> sortedPosts) {
        sortedPosts.forEach((post) -> {
            stream.send(getPayload(post), null);
        });
    }

    private String getPayload(final Post post) {
        return JSON_MAPPER.toJson(post);
    }

    private Connection<Post> query(final String page, final FacebookClient facebookClient, final Parameter... param1) {
        return facebookClient.fetchConnection(page + "/feed", Post.class, param1);
    }

    private Stream<Post> sortStreamByCreationTime(final List<Post> ls) {
        return ls.stream().sorted((o1, o2) -> o1.getCreatedTime().compareTo(o2.getCreatedTime()));
    }

    private List<Post> listToStream(final Connection<Post> pages) {
        final List<Post> ret = new ArrayList<>();
        for (List<Post> page : pages) {
            ret.addAll(page.stream().collect(Collectors.toList()));
        }
        return ret;
    }
}
