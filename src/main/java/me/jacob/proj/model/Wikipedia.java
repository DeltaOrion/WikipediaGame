package me.jacob.proj.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Wikipedia {

    private final AtomicInteger pageCount = new AtomicInteger(0);

    private final Map<String,WikiPage> byName;
    private final Map<WikiLink,WikiPage> byLink;

    /**
     * TODO
     *   - Better dependency injection with the fetcher
     *   - Better test fetcher which retrieves from an object
     *   - Begin transitioning to MVC
     *       - Wikipedia belongs to service layer, move update and create functions for wikipedia pages
     *       - Separate LinkService from LinkRepository
     *       - Create repositories for wikipedia and links
     *    Performance
     *      - Find out how long consumers and produces are blocked/starving
     *    Services
     *    - add UpdateWorkers which crawl through link repository
     *    - UpdateWorkers are released when the process is finished
     *    Controllers
     *    - Find CLI library.
     *    - Start by adding CLI controllers and views
     *
     *
     *
     */

    public static void main(String[] args) {
        Wikipedia wikipedia = new Wikipedia();
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

        wikipedia.addPage(one);
        wikipedia.addPage(two);
        wikipedia.addPage(three);
        wikipedia.addPage(four);
        wikipedia.addPage(five);


        wikipedia.calculateAllShortestPaths();
        four.setRemoved(true);
        System.out.println(wikipedia.getShortestPaths("2","4"));
    }

    public List<List<WikiPage>> getShortestPaths(String a, String b) {
        WikiPage A = byName.get(a);
        WikiPage B = byName.get(b);

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
            if(B.getUniqueId() > A.getAllPairShortest().size())
                return;

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

    public Wikipedia() {
        this.byName = new HashMap<>();
        this.byLink = new HashMap<>();
    }

    public synchronized void addPage(WikiPage page) {
        if(byName.containsKey(page.getTitle()))
            return;

        int nextPage = pageCount.getAndIncrement();
        this.byName.put(page.getTitle(),page);
        this.byLink.put(page.getLink(),page);
        page.setUniqueId(nextPage);
    }

    public synchronized WikiPage getPage(String title) {
        return byName.get(title);
    }

    public synchronized WikiPage getPage(WikiLink link) {
        return byLink.get(link);
    }

    public synchronized boolean hasPage(String page) {
        return this.byName.containsKey(page);
    }

    public synchronized boolean hasPage(WikiLink page) {
        return this.byLink.containsKey(page);
    }

    public synchronized Collection<WikiPage> getPages() {
        return Collections.unmodifiableCollection(byName.values());
    }

    public void calculateAllShortestPaths() {
        try {
            for (WikiPage page : byName.values()) {
                updatePaths(page);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePaths(WikiPage page) {
        Queue<WikiPage> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[byName.size()];
        visited[page.getUniqueId()] = true;
        queue.add(page);

        List<List<WikiPage>> allPairs = page.getAllPairShortest();
        allPairs.clear();

        for(int i = 0; i< byName.size(); i++) {
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
                    if(!visited[neighbour.getUniqueId()]) {
                        allPairs.get(neighbour.getUniqueId()).add(node);
                        queue.add(neighbour);
                        visited[neighbour.getUniqueId()] = true;
                        thisLayer[neighbour.getUniqueId()] = true;
                    } else if(thisLayer[neighbour.getUniqueId()]) {
                        //otherwise if it was added on this layer then add itself to the list.
                        allPairs.get(neighbour.getUniqueId()).add(node);
                    }
                }
            }
        }
    }

    public synchronized void update(WikiPage wikiPage, UpdateStatus status) {
        if(status.isUpdateTitle()) {
            byName.remove(status.getOldTitle());
            byName.put(wikiPage.getTitle(),wikiPage);
        }
    }
}
