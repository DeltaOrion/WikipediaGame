package me.jacob.proj.model.page;

import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class HashMapPageRepository implements PageRepository {

    private final Map<WikiLink, WikiPage> byLink;
    private final Map<String, WikiPage> byName;
    private final Map<Integer, WikiPage> byId;
    private final ReadWriteLock lock;

    private final AtomicInteger pageCount = new AtomicInteger(0);

    public HashMapPageRepository() {
        this.byLink = new HashMap<>();
        this.byName = new HashMap<>();
        this.byId = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public WikiPage getPage(String title) {
        try {
            lock.readLock().lock();
            return byName.get(title);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public WikiPage getPage(int id) {
        try {
            lock.readLock().lock();
            return byId.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public WikiPage getPage(WikiLink link) {
        try {
            lock.readLock().lock();
            return byLink.get(link);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<WikiPage> getAllPages() {
        try {
            lock.readLock().lock();
            return Collections.unmodifiableCollection(byName.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void createPage(WikiPage page) {
        try {
            lock.writeLock().lock();
            create(page);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void create(WikiPage page) {
        byLink.put(page.getLink(), page);
        byName.put(page.getTitle(), page);
        byId.put(page.getUniqueId(), page);
    }

    @Override
    public void createPages(Collection<WikiPage> pages) {
        try {
            lock.writeLock().lock();
            for (WikiPage page : pages) {
                create(page);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void savePage(WikiPage page) {

    }

    @Override
    public void updateName(String oldTitle, WikiPage page) {
        try {
            lock.writeLock().lock();
            byName.remove(oldTitle);
            byName.put(page.getTitle(), page);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getAmountOfPages() {
        try {
            lock.readLock().lock();
            return byName.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int nextUniqueId() {
        return pageCount.getAndIncrement();
    }

    private void clear() {

    }

    public Set<WikiPage> clearAndDo(Consumer<Set<WikiPage>> action) {
        try {
            lock.writeLock().lock();
            Set<WikiPage> existing = new HashSet<>(byName.values());
            action.accept(existing);

            byName.clear();
            byLink.clear();
            byId.clear();
            return existing;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
