package me.jacob.proj.model.map;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.LinkRepository;
import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HashMapLinkRepository implements LinkRepository {

    private final ConcurrentMap<WikiLink, CrawlableLink> linkMap;
    private final PageRepository pageRepo;

    public HashMapLinkRepository(PageRepository pageRepo) {
        this.pageRepo = pageRepo;
        this.linkMap = new ConcurrentHashMap<>();
    }

    @Override
    public CrawlableLink getOrMake(WikiLink link) {
        return linkMap.computeIfAbsent(link, link1 -> new CrawlableLink(link1, UUID.randomUUID(), pageRepo));
    }

    @Override
    public Collection<CrawlableLink> getOrMake(Collection<WikiLink> links) {
        List<CrawlableLink> found = new ArrayList<>();
        for(WikiLink link : links) {
            found.add(linkMap.computeIfAbsent(link, link1 -> new CrawlableLink(link1, UUID.randomUUID(), pageRepo)));
        }
        return found;
    }

    @Override
    public void updateAll(Collection<CrawlableLink> links, boolean updateConnected) {

    }

    @Override
    public CrawlableLink get(WikiLink link) {
        return linkMap.get(link);
    }

    @Override
    public Collection<CrawlableLink> getAll() {
        return Collections.unmodifiableCollection(linkMap.values());
    }

    @Override
    public void update(CrawlableLink link, boolean updateLinks) {

    }

    @Override
    public void create(CrawlableLink link) {
        linkMap.put(link.getLink(),link);
    }

    @Override
    public void delete(WikiLink link) {
        linkMap.remove(link);
    }


    @Override
    public int getAmountOfLinks() {
        return linkMap.size();
    }

    @Override
    public Collection<CrawlableLink> getBefore(long time) {
        List<CrawlableLink> links = new ArrayList<>();
        for(CrawlableLink link : getAll()) {
            if(link.getLastProcessed() < time) {
                links.add(link);
            }
        }
        return links;
    }

}
