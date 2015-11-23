package org.kevoree.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rometools.fetcher.FetcherEvent;
import com.rometools.fetcher.FetcherListener;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.apache.commons.lang3.ObjectUtils;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mleduc on 17/11/15.
 */
public class RSSEventListener implements FetcherListener {
    private final Port out;
    private Date currentDate;

    public RSSEventListener(final Port out) {
        this.out = out;
        this.currentDate = new Date();

    }

    @Override
    public void fetcherEvent(final FetcherEvent event) {
        try {
            final String eventType = event.getEventType();
            if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(eventType)) {
                Log.debug("RETRIEVED " + event.getUrlString());
                final SyndFeed feeds = event.getFeed();
                if (feeds != null && feeds.getEntries() != null) {
                    List<SyndEntry> feedsFiltered = feeds.getEntries().stream()
                            .filter((entry) -> getLastDate(entry) != null)
                            .filter((entry) ->
                                    getLastDate(entry) != null && getLastDate(entry).after(currentDate))
                            .sorted((entry1, entry2) -> ObjectUtils.compare(getLastDate(entry1), getLastDate(entry2)))
                            .collect(Collectors.toList());

                    if (!feedsFiltered.isEmpty()) {
                        currentDate = getLastDate(feedsFiltered.get(feedsFiltered.size() - 1));
                    }

                    feedsFiltered.forEach((entry) -> out.send(toJson(entry), null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Date getLastDate(SyndEntry entry) {
        if(entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate();
        } else {
            return entry.getPublishedDate();
        }
    }

    private String toJson(final SyndEntry entry) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        return gson.toJson(entry).toString();
    }
}
