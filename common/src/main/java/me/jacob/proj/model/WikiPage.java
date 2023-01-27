package me.jacob.proj.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WikiPage {

    //node data
    private int uniqueId;
    private String title;
    private String description;
    private final WikiLink link;

    private final PageRepository repository;
    private final ConcurrentMap<WikiPage,Object> cachedNeighbours;

    private boolean isRedirect;
    private String articleType;

    private boolean isRemoved;

    public WikiPage(String title, WikiLink link, PageRepository repository) {
        this.link = link;
        this.repository = repository;
        this.title = title;
        this.articleType = "";
        this.description = "";
        this.cachedNeighbours = new ConcurrentHashMap<>();
    }

    public synchronized String getTitle() {
        return title;
    }

    public synchronized String getDescription() {
        return description;
    }

    public Collection<WikiPage> getNeighbours() {
        Set<WikiPage> neighbours = new HashSet<>(repository.getNeighbours(this.uniqueId));
        neighbours.addAll(cachedNeighbours.keySet());
        return neighbours;
    }

    public Collection<WikiPage> getCachedNeighbours() {
        return Collections.unmodifiableCollection(cachedNeighbours.keySet());
    }

    public void addNeighbour(WikiPage neighbour) {
        cachedNeighbours.put(neighbour,new Object());
    }

    public synchronized void setDescription(String description) {
        this.description = description;
    }

    public WikiLink getLink() {
        return link;
    }

    public synchronized int getUniqueId() {
        return uniqueId;
    }

    public synchronized void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String toString() {
        return "WikiPage{" +
                "title='" + title + '\'' +
                '}';
    }

    public synchronized boolean isRedirect() {
        return isRedirect;
    }

    public synchronized void setRedirect(boolean redirect) {
        isRedirect = redirect;
    }

    public synchronized String getArticleType() {
        return articleType;
    }

    public synchronized void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public synchronized boolean isRemoved() {
        return isRemoved;
    }

    public synchronized void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public synchronized void setTitle(String title) {
        this.title = title;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WikiPage wikiPage = (WikiPage) o;
        return uniqueId == wikiPage.uniqueId && Objects.equals(link, wikiPage.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, link);
    }

    public void clearNeighbours() {
        this.cachedNeighbours.clear();
        repository.clearNeighbours(this.uniqueId);
    }

    public void clearCached() {
        this.cachedNeighbours.clear();
    }
}
