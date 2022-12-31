package me.jacob.proj.model;

import java.util.Collection;

public interface LinkRepository {

    CrawlableLink getOrMake(WikiLink link);

    Collection<CrawlableLink> getOrMake(Collection<WikiLink> links);

    CrawlableLink get(WikiLink link);

    Collection<CrawlableLink> getAll();

    void update(CrawlableLink link);

    void create(CrawlableLink link);

    void delete(WikiLink link);
}
