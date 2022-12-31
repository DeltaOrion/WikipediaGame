package me.jacob.proj.model.map;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.LinkRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.util.IDCounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class HashMapLinkRepository implements LinkRepository {

    private final ConcurrentMap<WikiLink, CrawlableLink> linkMap;
    private final IDCounter idCounter;

    public HashMapLinkRepository(IDCounter idCounter) {
        this.linkMap = new ConcurrentHashMap<>();
        this.idCounter = idCounter;
    }

    @Override
    public CrawlableLink getOrMake(WikiLink link) {
        return linkMap.computeIfAbsent(link, link1 -> new CrawlableLink(link1, idCounter.nextUniqueId()));
    }

    @Override
    public Collection<CrawlableLink> getOrMake(Collection<WikiLink> links) {
        List<CrawlableLink> found = new ArrayList<>();
        for(WikiLink link : links) {
            found.add(linkMap.computeIfAbsent(link, link1 -> new CrawlableLink(link1, idCounter.nextUniqueId())));
        }
        return found;
    }

    @Override
    public void updateAll(Collection<CrawlableLink> links) {

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

    @Override
    public int nextUniqueId() {
        return idCounter.nextUniqueId();
    }

    @Override
    public int getAmountOfLinks() {
        return linkMap.size();
    }

    @Override
    public Collection<CrawlableLink> getBetween(int minBlock, int maxBlock) {
        List<CrawlableLink> block = new ArrayList<>();
        for(CrawlableLink link : getAll()) {
            if(link.getUniqueId() >= minBlock && link.getUniqueId() <= maxBlock)
                block.add(link);
        }

        return block;
    }
}
