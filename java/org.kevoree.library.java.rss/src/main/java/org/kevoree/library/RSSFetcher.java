package org.kevoree.library;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.annotation.Update;

import com.rometools.fetcher.FeedFetcher;
import com.rometools.fetcher.FetcherException;
import com.rometools.fetcher.impl.FeedFetcherCache;
import com.rometools.fetcher.impl.HashMapFeedInfoCache;
import com.rometools.fetcher.impl.HttpURLFeedFetcher;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import org.kevoree.api.Context;
import org.kevoree.api.Port;


@ComponentType
public class RSSFetcher {

    @Param(defaultValue = "https://news.google.com/news?q=apple&output=rss", optional = false)
    public String url;

    @KevoreeInject
    private Context context;

    @Output
    public Port out;

    private FeedFetcher feedFetcher;

    @Input
    public void in(final Object inputValue) {
        try {
            final SyndFeed feed = feedFetcher.retrieveFeed(new URL(url));
            out.send(feed.toString(), null);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FeedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FetcherException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Start
    public void start() {
        final FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
        feedFetcher = new HttpURLFeedFetcher(feedInfoCache);


    }

    @Stop
    public void stop() {


    }

    @Update
    public void update() {
    }

}

