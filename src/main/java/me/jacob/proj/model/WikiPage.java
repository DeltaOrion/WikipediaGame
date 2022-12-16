package me.jacob.proj.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WikiPage {

    //node data
    private int uniqueId;
    private final String title;
    private String description;
    private final WikiLink link;
    private final List<WikiPage> neighbours;

    private boolean isRedirect;
    private String articleType;

    private boolean isRemoved;

    //path data
    private final List<List<WikiPage>> allPairShortest;

    public WikiPage(String title, WikiLink link) {
        this.link = link;
        this.neighbours = new ArrayList<>();
        this.title = title;

        this.allPairShortest = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public synchronized List<WikiPage> getNeighbours() {
        return Collections.unmodifiableList(neighbours);
    }

    public synchronized void addNeighbour(WikiPage neighbour) {
        this.neighbours.add(neighbour);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WikiLink getLink() {
        return link;
    }

    public List<List<WikiPage>> getAllPairShortest() {
        return allPairShortest;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String toString() {
        return title + " "+uniqueId;
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
}
