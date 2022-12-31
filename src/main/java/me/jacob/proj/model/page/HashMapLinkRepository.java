package me.jacob.proj.model.page;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.LinkRepository;
import me.jacob.proj.model.WikiLink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HashMapLinkRepository implements LinkRepository {

    private final ConcurrentMap<WikiLink, CrawlableLink> linkMap;

    public HashMapLinkRepository() {
        this.linkMap = new ConcurrentHashMap<>();
    }

    @Override
    public CrawlableLink getOrMake(WikiLink link) {
        return linkMap.computeIfAbsent(link, CrawlableLink::new);
    }

    @Override
    public Collection<CrawlableLink> getOrMake(Collection<WikiLink> links) {
        List<CrawlableLink> found = new ArrayList<>();
        for(WikiLink link : links) {
            found.add(linkMap.computeIfAbsent(link,CrawlableLink::new));
        }
        return found;
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
    public void update(CrawlableLink link) {

    }

    @Override
    public void create(CrawlableLink link) {
        linkMap.put(link.getLink(),link);
    }

    @Override
    public void delete(WikiLink link) {
        linkMap.remove(link);
    }
}
