package me.jacob.proj.service;

import me.jacob.proj.model.WikiPage;

import java.util.List;

public interface ShortestPathStrategy {

    List<List<WikiPage>> getShortestPaths(WikiPage start, WikiPage end);
}
