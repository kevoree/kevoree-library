package org.kevoree.rome;



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


@ComponentType
public class RomeFetcherComponent {

    @Param(defaultValue = "https://news.google.com/news?q=apple&output=rss")
    String url;

    @KevoreeInject
    org.kevoree.api.Context context;

    @Output
    org.kevoree.api.Port out;

	private FeedFetcher feedFetcher;

    @Input
    public void in(Object i) {
    	SyndFeed feed=null;
		try {
			feed = feedFetcher.retrieveFeed(new URL(url));
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
		out.send(feed.toString(),null);
    	
    }

    @Start
    public void start() {
    	FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
    	feedFetcher = new HttpURLFeedFetcher(feedInfoCache);

    	
    }

    @Stop
    public void stop() {
    	
    	
    	
    }

    @Update
    public void update() {}

}

