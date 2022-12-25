package me.jacob.proj.model;

import java.util.Collection;

public interface PageRepository {

    WikiPage getPage(String title);

    WikiPage getPage(int id);

    WikiPage getPage(WikiLink link);

    Collection<WikiPage> getAllPages();

    void createPage(WikiPage page);

    void createPages(Collection<WikiPage> pages);

    void savePage(WikiPage page);

    void updateName(String oldTitle ,WikiPage page);

    int getAmountOfPages();

    int nextUniqueId();
}
