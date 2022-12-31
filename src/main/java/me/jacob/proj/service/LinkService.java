package me.jacob.proj.service;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.LinkRepository;
import me.jacob.proj.model.WikiLink;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

public class LinkService {

    private Duration timeBetweenUpdates;
    private LinkRepository linkRepository;

    public LinkService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
        this.timeBetweenUpdates = Duration.of(100, ChronoUnit.SECONDS);
    }

    public boolean shouldBeCrawled(WikiLink link) {
        return shouldBeCrawled(get(link));
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

    public void createLink(WikiLink link) {
        if(linkRepository.get(link)!=null)
            return;

        linkRepository.create(new CrawlableLink(link));
    }

    public void deregister(WikiLink link) {
        CrawlableLink registeredLink = getOrMake(link);
        //this link shouldn't be checked until later
        //unless it should be crawled
        registeredLink.setProcessed(false);
    }

    public CrawlableLink getOrMake(WikiLink link) {
        return linkRepository.getOrMake(link);
    }

    public CrawlableLink get(WikiLink link) {
        return linkRepository.getOrMake(link);
    }

    public Collection<CrawlableLink> getAll() {
        return linkRepository.getAll();
    }

    public void update(CrawlableLink link) {
        linkRepository.update(link);
    }

    public void stash(WikiLink wikilink) {
        CrawlableLink registeredLink = getOrMake(wikilink);
        //there was a connection error, assume the registeredLink has been processed
        //this means it will be checked again at a later date.
        registeredLink.setProcessed(false);
    }

    public Duration getTimeBetweenUpdates() {
        return timeBetweenUpdates;
    }

    public void setTimeBetweenUpdates(Duration timeBetweenUpdates) {
        this.timeBetweenUpdates = timeBetweenUpdates;
    }

    public Collection<CrawlableLink> getOrMake(Collection<WikiLink> links) {
        return linkRepository.getOrMake(links);
    }
}
