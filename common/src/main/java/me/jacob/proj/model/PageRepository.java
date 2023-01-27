package me.jacob.proj.model;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface PageRepository {

    WikiPage getPage(String title);

    WikiPage getPage(int id);

    WikiPage getPage(WikiLink link);

    Collection<WikiPage> getAllPages();

    Collection<WikiPage> getAll(Collection<WikiLink> links);

    Collection<WikiPage> getNeighbours(int id);

    void createPage(WikiPage page);

    void createPages(Collection<WikiPage> pages);

    void savePage(WikiPage page, boolean updateLinks);

    void updateName(String oldTitle ,WikiPage page);

    int getAmountOfPages();

    int nextUniqueId();

    void clearNeighbours(int uniqueId);

    Collection<WikiPage> getAndClearUnconnected(UUID uniqueId);

    Collection<WikiPage> getUnconnected(UUID uniqueId);

    void saveUnconnected(Collection<CrawlableLink> links);

}
