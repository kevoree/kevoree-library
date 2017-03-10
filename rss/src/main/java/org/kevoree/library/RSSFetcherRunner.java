package org.kevoree.library;

import com.rometools.fetcher.FetcherException;
import com.rometools.fetcher.FetcherListener;
import com.rometools.fetcher.impl.FeedFetcherCache;
import com.rometools.fetcher.impl.HashMapFeedInfoCache;
import com.rometools.fetcher.impl.HttpURLFeedFetcher;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by mleduc on 17/11/15.
 */
public class RSSFetcherRunner implements Runnable {

    private final URL feedUrl;
    private final HttpURLFeedFetcher fetcher;

    public RSSFetcherRunner(final String rssUrl, final Port out) throws MalformedURLException {
        this.feedUrl = new URL(rssUrl);
        final FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
        this.fetcher = new HttpURLFeedFetcher(feedInfoCache);

        final FetcherListener listener = new RSSEventListener(out);

        this.fetcher.addFetcherEventListener(listener);


    }
    @Override
    public void run() {
        try {
            final SyndFeed feed = fetcher.retrieveFeed(feedUrl);
            Log.debug(feed.toString());
        } catch (IOException e) {
            Log.error(e.getMessage());
        } catch (FeedException e) {
            Log.error(e.getMessage());
        } catch (FetcherException e) {
            Log.error(e.getMessage());
        }
    }
}
