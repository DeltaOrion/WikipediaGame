package me.jacob.proj.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiPage {

    private final static AtomicInteger PAGE_COUNT = new AtomicInteger(0);

    //node data
    private final int uniqueId;
    private final String title;
    private String description;
    private final WikiLink link;
    private final List<WikiPage> neighbours;

    //path data
    private final List<List<WikiPage>> allPairShortest;

    public WikiPage(String title, WikiLink link) {
        this.uniqueId = PAGE_COUNT.getAndIncrement();
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

    public String toString() {
        return title + " "+uniqueId;
    }
}
