package me.jacob.proj.model;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LinkRepository {

    private final ConcurrentMap<WikiLink,CrawlableLink> links;
    private final Duration timeBetweenUpdates;

    public LinkRepository() {
        this.links = new ConcurrentHashMap<>();
        this.timeBetweenUpdates = Duration.of(100, ChronoUnit.SECONDS);
    }

    public boolean shouldBeCrawled(WikiLink link) {
        return shouldBeCrawled(links.get(link));
    }

    public boolean shouldBeCrawled(CrawlableLink registeredLink) {
        //if the link has never been registered then we should crawl it
        if(registeredLink==null)
            return true;
        //if the link has been registered to be crawled then we should not crawl it
        if(registeredLink.isRegistered())
            return false;

        //otherwise if it has been processed check the time between it last been processed
        return System.currentTimeMillis() - registeredLink.getLastProcessed() > timeBetweenUpdates.toMillis();
    }

    public void registerLink(WikiLink link) {
        links.putIfAbsent(link,new CrawlableLink(link));
    }

    public void deregister(WikiLink link) {
        links.remove(link);
    }

    public CrawlableLink getOrMake(WikiLink link) {
        return links.computeIfAbsent(link, CrawlableLink::new);
    }


    public void stash(WikiLink wikilink) {
        CrawlableLink link = getOrMake(wikilink);
        //there was a connection error, assume the link has been processed
        //this means it will be checked again at a later date.
        link.setProcessed();
    }
}
