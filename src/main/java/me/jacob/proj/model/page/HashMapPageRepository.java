package me.jacob.proj.model.page;

import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashMapPageRepository implements PageRepository {

    private final ConcurrentMap<WikiLink,WikiPage> byLink;
    private final ConcurrentMap<String,WikiPage> byName;
    private final ConcurrentMap<Integer,WikiPage> byId;

    private final AtomicInteger pageCount = new AtomicInteger(0);

    public HashMapPageRepository() {
        this.byLink = new ConcurrentHashMap<>();
        this.byName = new ConcurrentHashMap<>();
        this.byId = new ConcurrentHashMap<>();
    }

    @Override
    public WikiPage getPage(String title) {
        return byName.get(title);
    }

    @Override
    public WikiPage getPage(int id) {
        return byId.get(id);
    }

    @Override
    public WikiPage getPage(WikiLink link) {
        return byLink.get(link);
    }

    @Override
    public Collection<WikiPage> getAllPages() {
        return Collections.unmodifiableCollection(byName.values());
    }

    @Override
    public synchronized void createPage(WikiPage page) {
        byLink.put(page.getLink(),page);
        byName.put(page.getTitle(),page);
        byId.put(page.getUniqueId(),page);
    }

    @Override
    public void createPages(Collection<WikiPage> pages) {
        for(WikiPage page : pages) {
            createPage(page);
        }
    }

    @Override
    public void savePage(WikiPage page) {

    }

    @Override
    public synchronized void updateName(String oldTitle, WikiPage page) {
        byName.remove(oldTitle);
        byName.put(page.getTitle(),page);
    }

    @Override
    public int getAmountOfPages() {
        return byName.size();
    }

    @Override
    public int nextUniqueId() {
        return pageCount.getAndIncrement();
    }

    public synchronized void clear() {
        byName.clear();
        byLink.clear();
        byId.clear();
    }
}
