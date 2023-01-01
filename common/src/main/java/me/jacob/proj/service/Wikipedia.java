package me.jacob.proj.service;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.map.HashMapLinkRepository;
import me.jacob.proj.model.map.HashMapPageRepository;
import me.jacob.proj.util.AtomicIntCounter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Wikipedia {

    private final LinkService linkService;
    private final PageRepository repository;

    private final HashMapPageRepository bulkCreate;
    private final ConcurrentMap<CrawlableLink,Object> toSave;

    /**
     * TODO
     *   - Model
     *       - Create Neo4j or SQL data persistence solution
     *       - Lazy Loading for WikiPages neighbours
        - Controllers
            - Continue making commands
            - Commands which allow pages to be indexed
            - More options from the crawler, such as turning on and off debug mode
            - Add web front end. The front-end should be super simple and only capable of viewing
            pages and the path between them.
            - Web front end should launch a CLI and register some of its own commands
            - Display the actual graph
        - Configuration
            - Add a configuration for the crawler and link-service
            - Configuration should be a YAML config.
        Views
            - Display the graph of all the paths in the front-end
            - Add colours to the CLI
        - Logging
            - Add proper logging framework which also stores logs to a file
     */

    public static void main(String[] args) {
        LinkService service = new LinkService(new HashMapLinkRepository(new AtomicIntCounter()), 3);
        Wikipedia wikipedia = new Wikipedia(service, new HashMapPageRepository(new AtomicIntCounter()));
        WikiPage zero = new WikiPage("0",new WikiLink("0"));
        WikiPage one = new WikiPage("1",new WikiLink("1"));
        WikiPage two = new WikiPage("2",new WikiLink("2"));
        WikiPage three = new WikiPage("3",new WikiLink("3"));
        WikiPage four = new WikiPage("4",new WikiLink("4"));

        zero.addNeighbour(one);
        zero.addNeighbour(two);
        zero.addNeighbour(four);

        one.addNeighbour(three);
        one.addNeighbour(two);

        two.addNeighbour(three);
        two.addNeighbour(zero);

        three.addNeighbour(four);

        four.addNeighbour(three);
        four.addNeighbour(zero);

        wikipedia.create(zero);
        wikipedia.create(one);
        wikipedia.create(two);
        wikipedia.create(three);
        wikipedia.create(four);

        three.setRemoved(true);
        System.out.println(wikipedia.getShortestPaths("2","4"));
    }

    public Wikipedia(LinkService linkService, PageRepository repository) {
        this.linkService = linkService;
        this.repository = repository;
        this.bulkCreate = new HashMapPageRepository(repository::nextUniqueId);
        this.toSave = new ConcurrentHashMap<>();
    }


    public List<List<WikiPage>> getShortestPaths(String a, String b) {
        WikiPage A = getPage(a);
        WikiPage B = getPage(b);

        return getShortestPaths(A,B);
    }

    public List<List<WikiPage>> getShortestPaths(WikiPage start, WikiPage end) {
        if(start == null || end == null)
            return new ArrayList<>();

        ShortestPathStrategy strategy = new BFSShortestPathStrategy();
        return strategy.getShortestPaths(start,end);
    }



    public Collection<WikiLink> bulkCreate(WikiPage page, Collection<WikiLink> linksFound) {
        bulkCreate.createPage(page);
        page.setUniqueId(repository.nextUniqueId());
        UpdateStatus status = link(page,linksFound);
        this.toSave.put(status.getPageRegLink(),new Object());
        return status.getUnindexed();
    }

    public void publishBulkCreate() {
        bulkCreate.clearAndDo(repository::createPages);
        linkService.update(toSave.keySet());
        toSave.clear();
    }
    //we could also return a create status with more detailed information in the future
    public Collection<WikiLink> create(WikiPage page, Collection<WikiLink> linksFound) {
        if(repository.getPage(page.getLink())!=null)
            return new ArrayList<>();

        UpdateStatus update = link(page,linksFound);
        page.setUniqueId(repository.nextUniqueId());
        repository.createPage(page);
        linkService.update(update.getPageRegLink());
        return update.getUnindexed();
    }

    public void create(WikiPage page) {
        if(getPage(page.getLink())!=null)
            return;

        page.setUniqueId(repository.nextUniqueId());
        repository.createPage(page);
    }

    //we could also return an update status with more detailed information in the future.
    public Collection<WikiLink> update(WikiPage page, String title, String description, boolean isRedirect, String articleType, Collection<WikiLink> linksFound) {
        Collection<WikiLink> unindexed = updateLinks(page,linksFound);
        updateTitle(page,title);
        page.setDescription(description);
        page.setRedirect(isRedirect);
        page.setArticleType(articleType);

        CrawlableLink link = linkService.get(page.getLink());
        link.setProcessed(true);

        linkService.update(link);
        repository.savePage(page);
        return unindexed;
    }

    private void updateTitle(WikiPage page, String title) {
        if(!page.getTitle().equals(title)) {
            String oldTitle = page.getTitle();
            page.setTitle(title);
            repository.updateName(oldTitle,page);
        }
    }

    private Collection<WikiLink> updateLinks(WikiPage page, Collection<WikiLink> linksFound) {
        if(linksFound.size() == page.getNeighbours().size()) {
            Set<WikiLink> linksSet = new HashSet<>(linksFound);
            for(WikiPage p : page.getNeighbours()) {
                linksSet.remove(p.getLink());
            }

            //the links are not the same, relink
            if(linksSet.size()!=0) {
                page.clearNeighbours();
                return link(page,linksFound).getUnindexed();
            }
        }

        return new ArrayList<>();
    }

    private UpdateStatus link(WikiPage page, Collection<WikiLink> links) {
        CrawlableLink pageRegLink = linkService.getOrMake(page.getLink());
        pageRegLink.setProcessed(true);

        List<WikiLink> unindexed = new ArrayList<>();
        for (WikiLink link : links) {
            WikiPage pageLink = getPage(link);
            if (pageLink != null) {
                page.addNeighbour(pageLink);
            } else {
                //in this case the link has not been crawled yet
                //we need to add it to the unconnectedEdged
                //add the link to be crawled
                unindexed.add(link);
            }
        }

        for (WikiLink link : unindexed) {
            CrawlableLink registered = linkService.getOrMake(link);
            registered.addUnconnected(page);
        }

        //the unconnected link to this page. Add the links!
        Collection<WikiPage> unconnected = null;
        unconnected = pageRegLink.getAndUnlink();

        if (unconnected != null) {
            for (WikiPage p : unconnected) {
                p.addNeighbour(page);
            }
        }

        return new UpdateStatus(unindexed,pageRegLink);
    }

    public void remove(WikiLink link) {
        linkService.deregister(link);
        //if we have already found a page, then remove it
        WikiPage page = getPage(link);
        remove(page);
    }

    public void remove(WikiPage page) {
        if(page==null)
            return;

        page.setRemoved(true);
        repository.savePage(page);
    }

    public WikiPage getPage(String title) {
        WikiPage page = bulkCreate.getPage(title);
        if(page==null) {
            return repository.getPage(title);
        }
        return page;
    }

    public WikiPage getPage(WikiLink link) {
        WikiPage page = bulkCreate.getPage(link);
        if(page==null) {
            return repository.getPage(link);
        }
        return page;
    }

    public WikiPage getPage(int uniqueId) {
        WikiPage page = bulkCreate.getPage(uniqueId);
        if(page==null) {
            return repository.getPage(uniqueId);
        }
        return page;
    }

    public Collection<WikiPage> getAllPages() {
        publishBulkCreate();
        return repository.getAllPages();
    }

    public int size() {
        return repository.getAmountOfPages() + bulkCreate.getAmountOfPages();
    }

    public static class UpdateStatus {
        private final Collection<WikiLink> unindexed;
        private final CrawlableLink pageRegLink;

        public UpdateStatus(Collection<WikiLink> unindexed, CrawlableLink pageRegLink) {
            this.unindexed = unindexed;
            this.pageRegLink = pageRegLink;
        }

        public Collection<WikiLink> getUnindexed() {
            return unindexed;
        }

        public CrawlableLink getPageRegLink() {
            return pageRegLink;
        }
    }
}
