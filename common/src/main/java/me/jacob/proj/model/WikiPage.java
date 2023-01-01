package me.jacob.proj.model;

import java.util.*;

public class WikiPage {

    //node data
    private int uniqueId;
    private String title;
    private String description;
    private final WikiLink link;
    private final List<WikiPage> neighbours;

    private boolean isRedirect;
    private String articleType;

    private boolean isRemoved;

    public WikiPage(String title, WikiLink link) {
        this.link = link;
        this.neighbours = new ArrayList<>();
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public synchronized String getDescription() {
        return description;
    }

    public synchronized List<WikiPage> getNeighbours() {
        return Collections.unmodifiableList(neighbours);
    }

    public synchronized void addNeighbour(WikiPage neighbour) {
        this.neighbours.add(neighbour);
    }

    public synchronized void setDescription(String description) {
        this.description = description;
    }

    public WikiLink getLink() {
        return link;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String toString() {
        return "WikiPage{" +
                "title='" + title + '\'' +
                '}';
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public void setRedirect(boolean redirect) {
        isRedirect = redirect;
    }

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WikiPage wikiPage = (WikiPage) o;
        return uniqueId == wikiPage.uniqueId && Objects.equals(description, wikiPage.description) && Objects.equals(link, wikiPage.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, description, link);
    }

    public synchronized void clearNeighbours() {
        this.neighbours.clear();
    }
}
