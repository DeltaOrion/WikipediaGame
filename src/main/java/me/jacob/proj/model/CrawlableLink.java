package me.jacob.proj.model;

import java.util.*;

public class CrawlableLink {

    private Set<WikiPage> unconnectedEdges;
    private final WikiLink link;
    private boolean processed;
    private boolean registered;
    private boolean pageFound;
    private long lastProcessed;

    public CrawlableLink(WikiLink link) {
        this.link = link;
        this.processed = false;
        this.registered = false;
        this.pageFound = false;
        this.lastProcessed = -1;
        this.unconnectedEdges = new HashSet<>();
    }

    public WikiLink getLink() {
        return link;
    }

    public synchronized boolean isProcessed() {
        return processed;
    }

    public synchronized void setProcessed(boolean pageFound) {
        this.processed = true;
        this.registered = false;
        this.pageFound = pageFound;
        this.lastProcessed = System.currentTimeMillis();
    }

    public long getLastProcessed() {
        return lastProcessed;
    }

    public synchronized void addUnconnected(WikiPage page) {
        unconnectedEdges.add(page);
    }

    public synchronized void unlink() {
        unconnectedEdges = new HashSet<>();
    }

    public synchronized Collection<WikiPage> getAndUnlink() {
        List<WikiPage> pages = new ArrayList<>(unconnectedEdges);
        unconnectedEdges.clear();
        return pages;
    }

    public synchronized Collection<WikiPage> getUnconnected() {
        return Collections.unmodifiableSet(unconnectedEdges);
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isPageFound() {
        return pageFound;
    }

    public void setPageFound(boolean pageFound) {
        this.pageFound = pageFound;
    }
}
