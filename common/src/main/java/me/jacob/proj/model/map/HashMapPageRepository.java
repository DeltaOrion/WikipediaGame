package me.jacob.proj.model.map;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.util.IDCounter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class HashMapPageRepository implements PageRepository {

    private final Map<WikiLink, WikiPage> byLink;
    private final Map<String, WikiPage> byName;
    private final Map<Integer, WikiPage> byId;
    private final ReadWriteLock lock;
    private final IDCounter idCounter;

    private final Map<Integer,List<WikiPage>> neighbours = new HashMap<>();
    private final Map<UUID,List<WikiPage>> unconnected = new ConcurrentHashMap<>();

    public HashMapPageRepository(IDCounter idCounter) {
        this.byLink = new HashMap<>();
        this.byName = new HashMap<>();
        this.byId = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.idCounter = idCounter;
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
    public Collection<WikiPage> getAll(Collection<WikiLink> links) {
        try {
            lock.readLock().lock();
            List<WikiPage> result = new ArrayList<>();
            for (WikiLink link : links) {
                WikiPage page = byLink.get(link);
                if(page!=null)
                    result.add(page);
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<WikiPage> getNeighbours(int id) {
        try {
            lock.readLock().lock();
            List<WikiPage> n = this.neighbours.get(id);
            if (n == null) {
                n = new ArrayList<>();
                neighbours.put(id, n);
            }
            return n;

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
        neighbours.put(page.getUniqueId(),new ArrayList<>());
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
    public void savePage(WikiPage page, boolean updateLinks) {

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
        return idCounter.nextUniqueId();
    }

    @Override
    public void clearNeighbours(int uniqueId) {
        try {
            lock.writeLock().lock();
            neighbours.remove(uniqueId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public synchronized Collection<WikiPage> getAndClearUnconnected(UUID uniqueId) {
        List<WikiPage> unconnected = this.unconnected.remove(uniqueId);
        if(unconnected==null)
            return new ArrayList<>();

        return unconnected;
    }

    @Override
    public synchronized Collection<WikiPage> getUnconnected(UUID uniqueId) {
        List<WikiPage> unconnected = this.unconnected.get(uniqueId);
        if(unconnected==null)
            return new ArrayList<>();

        return unconnected;
    }

    @Override
    public synchronized void saveUnconnected(Collection<CrawlableLink> links) {
        for(CrawlableLink link : links) {
            List<WikiPage> found = this.unconnected.computeIfAbsent(link.getUniqueId(), k -> new ArrayList<>());
            found.clear();
            found.addAll(link.getUnconnected());
        }
    }

    public Set<WikiPage> clearAndDo(Consumer<Set<WikiPage>> action) {
        try {
            lock.writeLock().lock();
            Set<WikiPage> existing = new HashSet<>(byName.values());
            action.accept(existing);

            byName.clear();
            byLink.clear();
            byId.clear();
            neighbours.clear();
            return existing;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
