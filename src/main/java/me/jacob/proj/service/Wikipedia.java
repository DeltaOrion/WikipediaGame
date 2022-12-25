package me.jacob.proj.service;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.page.HashMapPageRepository;

import java.util.*;

public class Wikipedia {

    private final LinkService linkService;
    private final PageRepository repository;

    private final HashMapPageRepository bulkCreate;

    /**
     * TODO
     *   - Begin transitioning to MVC
     *       - Wikipedia belongs to service layer, move update and create functions for wikipedia pages
     *       - Separate LinkService from LinkRepository
     *       - Create repositories for wikipedia and links
     *       - Create Neo4j data persistance solution
     *    Performance
     *      - Stop precalculating all of the shortest paths and instead calculate it on demand using Neo4j or a BFS search
     *    Services
     *    - add UpdateWorkers which crawl through link repository
     *    - UpdateWorkers are released when the process is finished
     *    - Add a system for calculating the shortest path (batches of 10000 for example)
     *    Controllers
     *    - Find CLI library.
     *    - Start by adding CLI controllers and views
     */

    public static void main(String[] args) {
        LinkService service = new LinkService();
        Wikipedia wikipedia = new Wikipedia(service, new HashMapPageRepository());
        WikiPage one = new WikiPage("0",new WikiLink("1"));
        WikiPage two = new WikiPage("1",new WikiLink("2"));
        WikiPage three = new WikiPage("2",new WikiLink("3"));
        WikiPage four = new WikiPage("3",new WikiLink("4"));
        WikiPage five = new WikiPage("4",new WikiLink("5"));

        one.addNeighbour(two);
        one.addNeighbour(three);
        one.addNeighbour(five);

        two.addNeighbour(four);
        two.addNeighbour(three);

        three.addNeighbour(four);
        three.addNeighbour(one);

        four.addNeighbour(five);
        five.addNeighbour(four);

        five.addNeighbour(one);

        wikipedia.create(one);
        wikipedia.create(two);
        wikipedia.create(three);
        wikipedia.create(four);
        wikipedia.create(five);


        wikipedia.calculateAllShortestPaths();
        four.setRemoved(true);
        System.out.println(wikipedia.getShortestPaths("2","4"));
    }

    public Wikipedia(LinkService linkService, PageRepository repository) {
        this.linkService = linkService;
        this.repository = repository;
        this.bulkCreate = new HashMapPageRepository();
    }


    public List<List<WikiPage>> getShortestPaths(String a, String b) {
        WikiPage A = getPage(a);
        WikiPage B = getPage(b);

        if(A == null || B == null)
            return new ArrayList<>();

        List<List<WikiPage>> result = new ArrayList<>();
        List<WikiPage> currPath = new ArrayList<>();
        currPath.add(B);
        getShortestPaths(A,B,result,currPath);
        return result;
    }

    private void getShortestPaths(WikiPage A, WikiPage B, List<List<WikiPage>> result, List<WikiPage> currPath) {
        if(A.equals(B)) {
            List<WikiPage> path = new ArrayList<>();
            for(int i=currPath.size()-1;i>=0;i--) {
                path.add(currPath.get(i));
            }
            result.add(path);
        } else {
            //this hasn't been indexed yet! - Stop
            //if(B.getUniqueId() > A.getAllPairShortest().size())
            //    return;

            //this node has been removed, don't include it when finding the paths
            if(B.isRemoved())
                return;

            for(WikiPage path : A.getAllPairShortest().get(B.getUniqueId())) {
                currPath.add(path);
                getShortestPaths(A,path,result,currPath);
                currPath.remove(currPath.size()-1);
            }
        }
    }

    public void calculateAllShortestPaths() {
        try {
            for (WikiPage page : repository.getAllPages()) {
                updatePaths(page);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePaths(WikiPage page) {
        Queue<WikiPage> queue = new ArrayDeque<>();
        int noOfPages = repository.getAmountOfPages();
        boolean[] visited = new boolean[noOfPages];
        visited[page.getUniqueId()] = true;
        queue.add(page);

        List<List<WikiPage>> allPairs = page.getAllPairShortest();
        allPairs.clear();

        for(int i = 0; i< noOfPages; i++) {
            allPairs.add(new ArrayList<>());
        }

        allPairs.get(page.getUniqueId()).add(page);

        while (!queue.isEmpty()) {
            int size = queue.size();
            boolean[] thisLayer = new boolean[visited.length];
            for(int i=0;i<size;i++) {
                WikiPage node = queue.poll();
                for(WikiPage neighbour : node.getNeighbours()) {
                    //we want the node to add itself to the list for all of its neighbours if either
                    //the node is unvisited or if it has been visited, it was added in this layer!
                    //if(neighbour.getUniqueId() < node.getNeighbours().size()) {
                    if(!visited[neighbour.getUniqueId()]) {
                        allPairs.get(neighbour.getUniqueId()).add(node);
                        queue.add(neighbour);
                        visited[neighbour.getUniqueId()] = true;
                        thisLayer[neighbour.getUniqueId()] = true;
                    } else if(thisLayer[neighbour.getUniqueId()]) {
                        //otherwise if it was added on this layer then add itself to the list.
                        allPairs.get(neighbour.getUniqueId()).add(node);
                    }
                    //}
                }
            }
        }

        repository.savePage(page);
    }



    public Collection<WikiLink> bulkCreate(WikiPage page, Collection<WikiLink> linksFound) {
        synchronized (this) {
            bulkCreate.createPage(page);
        }

        page.setUniqueId(repository.nextUniqueId());
        return link(page,linksFound);
    }

    public void publishBulkCreate() {
        Set<WikiPage> bulkUpdate = null;
        synchronized (this) {
            bulkUpdate = new HashSet<>(bulkCreate.getAllPages());
            bulkCreate.clear();
        }

        if(bulkUpdate.size()==0)
            return;
        repository.createPages(bulkUpdate);
    }
    //we could also return a create status with more detailed information in the future
    public Collection<WikiLink> create(WikiPage page, Collection<WikiLink> linksFound) {
        if(repository.getPage(page.getLink())!=null)
            return new ArrayList<>();

        Collection<WikiLink> unindexed = link(page,linksFound);
        page.setUniqueId(repository.nextUniqueId());
        repository.createPage(page);
        return unindexed;
    }

    public void create(WikiPage page) {
        if(repository.getPage(page.getLink())!=null)
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
                return link(page,linksFound);
            }
        }

        return new ArrayList<>();
    }

    private Collection<WikiLink> link(WikiPage page, Collection<WikiLink> links) {
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

        return unindexed;
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
        WikiPage page = repository.getPage(title);
        if(page==null) {
            return bulkCreate.getPage(title);
        }
        return page;
    }

    public WikiPage getPage(WikiLink link) {
        WikiPage page = repository.getPage(link);
        if(page==null) {
            return bulkCreate.getPage(link);
        }
        return page;
    }

    public WikiPage getPage(int uniqueId) {
        WikiPage page = repository.getPage(uniqueId);
        if(page==null) {
            return bulkCreate.getPage(uniqueId);
        }
        return page;
    }

    public Collection<WikiPage> getAllPages() {
        publishBulkCreate();
        return repository.getAllPages();
    }
}
