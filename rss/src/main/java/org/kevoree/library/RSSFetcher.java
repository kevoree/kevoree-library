package org.kevoree.library;


import org.kevoree.annotation.*;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.net.MalformedURLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@ComponentType(version = 1)
public class RSSFetcher {

    @Param(optional = false)
    public String url = "https://news.google.com/news?q=apple&output=rss";

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

