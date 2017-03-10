package org.kevoree.library;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.kevoree.log.Log;


@ComponentType(version = 1)
public class RSSFetcher {

    @Param(defaultValue = "https://news.google.com/news?q=apple&output=rss", optional = false)
    public String url;

    @Output
    public Port out;


    private ScheduledExecutorService service;

    @Start
    public void start() {
        this.service = Executors.newScheduledThreadPool(1);
        try {
            service.scheduleWithFixedDelay(new RSSFetcherRunner(this.url, this.out), 0, 15, TimeUnit.MINUTES);
        } catch (MalformedURLException e) {
            Log.error(e.getMessage());
        }
    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    @Stop
    private void stop() {
        this.service.shutdown();
    }
}

