package me.jacob.proj.model;

import java.util.*;

public class CrawlableLink {

    private final UUID uniqueId;
    private Set<WikiPage> cachedUnconnected;
    private final PageRepository pageRepository;
    private final WikiLink link;
    private boolean processed;
    private boolean registered;
    private boolean pageFound;
    private long lastProcessed;

    public CrawlableLink(WikiLink link, UUID uniqueId, PageRepository pageRepository) {
        this.link = link;
        this.pageRepository = pageRepository;
        this.processed = false;
        this.registered = false;
        this.pageFound = false;
        this.lastProcessed = -1;
        this.cachedUnconnected = new HashSet<>();
        this.uniqueId = uniqueId;
    }

    public WikiLink getLink() {
        return link;
    }

    public synchronized boolean isProcessed() {
        return processed;
    }

    public synchronized void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public synchronized void toggleProcessed(boolean pageFound) {
        this.processed = true;
        this.registered = false;
        this.pageFound = pageFound;
        this.lastProcessed = System.currentTimeMillis();
    }

    public long getLastProcessed() {
        return lastProcessed;
    }

    public synchronized void addUnconnected(WikiPage page) {
        cachedUnconnected.add(page);
    }

    public synchronized void unlink() {
        cachedUnconnected = new HashSet<>();
    }

    public synchronized Collection<WikiPage> getAndUnlink() {
        List<WikiPage> pages = new ArrayList<>(cachedUnconnected);
        cachedUnconnected.clear();
        pages.addAll(pageRepository.getAndClearUnconnected(this.uniqueId));
        return pages;
    }

    public synchronized Collection<WikiPage> getUnconnected() {
        Set<WikiPage> neighbours = new HashSet<>(pageRepository.getUnconnected(this.uniqueId));
        neighbours.addAll(cachedUnconnected);
        return neighbours;
    }

    public synchronized Collection<WikiPage> getCachedUnconnected() {
        return cachedUnconnected;
    }

    public synchronized boolean isRegistered() {
        return registered;
    }

    public synchronized void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public synchronized boolean isPageFound() {
        return pageFound;
    }

    public synchronized void setPageFound(boolean pageFound) {
        this.pageFound = pageFound;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String toString() {
        return "CrawlableLink{" +
                "uniqueId=" + uniqueId +
                ", link=" + link +
                ", processed=" + processed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawlableLink link = (CrawlableLink) o;
        return uniqueId.equals(link.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    public synchronized void setLastProcessed(long lastProcessed) {
        this.lastProcessed = lastProcessed;
    }
}
