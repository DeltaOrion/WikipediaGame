package me.jacob.proj.model;

import java.util.*;

public class CrawlableLink {

    private Set<WikiPage> unconnectedEdges;
    private final WikiLink link;
    private boolean processed;
    private boolean registered;
    private long lastProcessed;

    public CrawlableLink(WikiLink link) {
        this.link = link;
        this.processed = false;
        this.registered = false;
        this.lastProcessed = -1;
        this.unconnectedEdges = new HashSet<>();
    }

    public WikiLink getLink() {
        return link;
    }

    public synchronized boolean isProcessed() {
        return processed;
    }

    public synchronized void setProcessed() {
        this.processed = true;
        registered = false;
        lastProcessed = System.currentTimeMillis();
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

    public Collection<WikiPage> getUnconnected() {
        return Collections.unmodifiableSet(unconnectedEdges);
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
