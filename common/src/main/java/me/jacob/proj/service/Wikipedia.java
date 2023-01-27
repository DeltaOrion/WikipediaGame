package me.jacob.proj.service;

import me.jacob.proj.model.*;
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
     *       - Add Neo4jLinkRepository
     *       - Create real bulk update for page creation
     *       - Add way to match ignorecase for wikipages
     *  - Service
     *       - Use a better method for getting the lins that need to be updated
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
        PageRepository repository = new HashMapPageRepository(new AtomicIntCounter());
        LinkService service = new LinkService(new HashMapLinkRepository(repository), repository);
        Wikipedia wikipedia = new Wikipedia(service, repository);
        WikiPage zero = new WikiPage("0",new WikiLink("0"), repository);
        WikiPage one = new WikiPage("1",new WikiLink("1"), repository);
        WikiPage two = new WikiPage("2",new WikiLink("2"), repository);
        WikiPage three = new WikiPage("3",new WikiLink("3"), repository);
        WikiPage four = new WikiPage("4",new WikiLink("4"), repository);
        WikiPage five = new WikiPage("5",new WikiLink("5"), repository);
        WikiPage six = new WikiPage("6",new WikiLink("6"), repository);

        zero.addNeighbour(one);
        zero.addNeighbour(two);
        zero.addNeighbour(three);
        zero.addNeighbour(four);

        one.addNeighbour(four);
        two.addNeighbour(five);
        three.addNeighbour(five);

        five.addNeighbour(six);
        four.addNeighbour(six);

        /*
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
        */


        wikipedia.create(zero);
        wikipedia.create(one);
        wikipedia.create(two);
        wikipedia.create(three);
        wikipedia.create(four);
        wikipedia.create(five);
        wikipedia.create(six);

        //three.setRemoved(true);
        System.out.println(wikipedia.getShortestPaths("0","0"));
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
        page.setUniqueId(repository.nextUniqueId());
        bulkCreate.createPage(page);
        UpdateStatus status = link(page,linksFound);
        this.toSave.put(status.getPageRegLink(),new Object());
        return status.getUnindexed();
    }

    public void publishBulkCreate() {
        bulkCreate.clearAndDo(repository::createPages);
        linkService.update(toSave.keySet(),true);
        toSave.clear();
    }
    //we could also return a create status with more detailed information in the future
    public Collection<WikiLink> create(WikiPage page, Collection<WikiLink> linksFound) {
        page.setUniqueId(repository.nextUniqueId());
        UpdateStatus update = link(page,linksFound);
        repository.createPage(page);
        linkService.update(update.getPageRegLink(),true);
        return update.getUnindexed();
    }

    public WikiPage newPage(String title, WikiLink link) {
        return new WikiPage(title,link,repository);
    }

    public void create(WikiPage page) {
        if(getPage(page.getLink())!=null)
            return;

        page.setUniqueId(repository.nextUniqueId());
        repository.createPage(page);
    }

    //we could also return an update status with more detailed information in the future.
    public Collection<WikiLink> update(WikiPage page, String title, String description, boolean isRedirect, String articleType, Collection<WikiLink> linksFound) {
        boolean update = false;

        UpdateStatus status = updateLinks(page,linksFound);
        if(status.updateLinks)
            update = true;

        if(updateTitle(page,title))
            update = true;

        if(!description.equals(page.getDescription())) {
            page.setDescription(description);
            update = true;
        }

        if(isRedirect != page.isRedirect()) {
            page.setRedirect(isRedirect);
            update = true;
        }

        if(!articleType.equals(page.getArticleType())) {
            page.setArticleType(articleType);
            update = true;
        }



        if(update) {
            CrawlableLink link = status.getPageRegLink();
            if(link!=null) {
                linkService.update(link, status.updateLinks);
                link.toggleProcessed(true);
            }
            repository.savePage(page,status.updateLinks);
        }

        return status.getUnindexed();
    }

    private boolean updateTitle(WikiPage page, String title) {
        if(page.getTitle().equals(title))
            return false;

        String oldTitle = page.getTitle();
        page.setTitle(title);
        repository.updateName(oldTitle,page);
        return true;
    }

    private UpdateStatus updateLinks(WikiPage page, Collection<WikiLink> linksFound) {
        Collection<WikiPage> neighbours = page.getNeighbours();
        if(linksFound.size() != neighbours.size()) {
            page.clearNeighbours();
            return link(page,linksFound);
        }

        Set<WikiLink> linksSet = new HashSet<>(linksFound);
        for(WikiPage p : neighbours) {
            linksSet.remove(p.getLink());
        }

        //the links are not the same, relink
        if(linksSet.size()!=0) {
            page.clearNeighbours();
            return link(page,linksFound);
        }

        return new UpdateStatus(new ArrayList<>(),null,false);
    }

    private UpdateStatus link(WikiPage page, Collection<WikiLink> links) {
        CrawlableLink pageRegLink = linkService.getOrMake(page.getLink());
        pageRegLink.toggleProcessed(true);

        List<WikiLink> unindexed = new ArrayList<>();
        Collection<WikiPage> pages = getAll(links);
        Map<WikiLink,WikiPage> byLink = new HashMap<>();
        for(WikiPage p : pages) {
            byLink.put(p.getLink(),p);
        }
        for (WikiLink link : links) {
            //culprit method - blocking call made thousands of times
            WikiPage pageLink = byLink.get(link);
            if (pageLink != null) {
                page.addNeighbour(pageLink);
            } else {
                //in this case the link has not been crawled yet
                //we need to add it to the unconnectedEdged
                //add the link to be crawled
                unindexed.add(link);
            }
        }

        Collection<CrawlableLink> crawlableLinks = linkService.getOrMake(unindexed);
        for(CrawlableLink registered : crawlableLinks) {
            registered.addUnconnected(page);
            //these need to be saved
        }

        linkService.update(crawlableLinks,true);

        //the unconnected link to this page. Add the links!
        Collection<WikiPage> unconnected = pageRegLink.getAndUnlink();

        if (unconnected != null) {
            for (WikiPage p : unconnected) {
                p.addNeighbour(page);
            }
        }

        repository.createPages(unconnected);

        return new UpdateStatus(unindexed,pageRegLink,true);
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
        repository.savePage(page,false);
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

    private Collection<WikiPage> getAll(Collection<WikiLink> links) {
        Set<WikiPage> all = new HashSet<>(bulkCreate.getAll(links));
        all.addAll(repository.getAll(links));
        return all;
    }

    public int size() {
        return repository.getAmountOfPages() + bulkCreate.getAmountOfPages();
    }

    public static class UpdateStatus {
        private final Collection<WikiLink> unindexed;
        private final CrawlableLink pageRegLink;
        private boolean updateLinks;

        public UpdateStatus(Collection<WikiLink> unindexed, CrawlableLink pageRegLink, boolean updateLinks) {
            this.unindexed = unindexed;
            this.pageRegLink = pageRegLink;
            this.updateLinks = updateLinks;
        }

        public Collection<WikiLink> getUnindexed() {
            return unindexed;
        }

        public CrawlableLink getPageRegLink() {
            return pageRegLink;
        }


    }
}
