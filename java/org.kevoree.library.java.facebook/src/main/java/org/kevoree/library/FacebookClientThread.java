package org.kevoree.library;

import com.restfb.*;
import com.restfb.types.Post;
import org.kevoree.api.Port;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mleduc on 16/11/15.
 */
public class FacebookClientThread extends Thread {

    private final String accessToken;
    private final String page;
    private final Port stream;
    private boolean stop = false;
    public static final JsonMapper JSON_MAPPER = new DefaultJsonMapper();

    public FacebookClientThread(final String accessToken, final String page, final Port stream) {
        this.accessToken = accessToken;
        this.page = page;
        this.stream = stream;
    }

    @Override
    public void run() {
        final FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.VERSION_2_5);

        long latestTime = System.currentTimeMillis();

        while(!stop) {
            final List<Post> sortedPosts = getPosts(page, facebookClient, getParamSince(latestTime));
            send(sortedPosts);
            final Post latest = sortedPosts.get(sortedPosts.size()-1);
            latestTime = latest.getCreatedTime().getTime();
        }
    }

    private Parameter getParamSince(final long time) {
        return Parameter.with("since", time / 1000);
    }

    private List<Post> getPosts(final String page, final FacebookClient facebookClient, final Parameter... params) {
        List<Post> sortedPosts;
        while (true) {
            final Connection<Post> posts = query(page, facebookClient, params);
            if (posts.getData().isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            } else {
                sortedPosts = sortStreamByCreationTime(listToStream(posts)).collect(Collectors.toList());
                break;
            }
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

    public void askStop() {
        this.stop = true;
    }
}
