package me.jacob.proj.model;

import java.util.Collection;

public interface LinkRepository {

    CrawlableLink getOrMake(WikiLink link);

    Collection<CrawlableLink> getOrMake(Collection<WikiLink> links);

    void updateAll(Collection<CrawlableLink> links, boolean updateConnected);

    CrawlableLink get(WikiLink link);

    Collection<CrawlableLink> getAll();

    void update(CrawlableLink link, boolean updateConnected);

    void create(CrawlableLink link);

    void delete(WikiLink link);

    int getAmountOfLinks();

    Collection<CrawlableLink> getBefore(long time);

}
