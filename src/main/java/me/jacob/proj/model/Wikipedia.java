package me.jacob.proj.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Wikipedia {

    private final static AtomicInteger PAGE_COUNT = new AtomicInteger(0);

    private final Map<String,WikiPage> pages;
    private final Map<WikiLink,WikiPage> links;

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
        System.out.println(wikipedia.getShortestPaths("2","4"));
    }

    public List<List<WikiPage>> getShortestPaths(String a, String b) {
        WikiPage A = pages.get(a);
        WikiPage B = pages.get(b);

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
            //this hasn't been indexed yet!
            //stop
            if(B.getUniqueId() > A.getAllPairShortest().size())
                return;

            for(WikiPage path : A.getAllPairShortest().get(B.getUniqueId())) {
                currPath.add(path);
                getShortestPaths(A,path,result,currPath);
                currPath.remove(currPath.size()-1);
            }
        }
    }

    public Wikipedia() {
        this.pages = new HashMap<>();
        this.links = new HashMap<>();
    }

    public synchronized void addPage(WikiPage page) {
        int nextPage = PAGE_COUNT.getAndIncrement();
        this.pages.put(page.getTitle(),page);
        this.links.put(page.getLink(),page);
        page.setUniqueId(nextPage);
    }

    public synchronized WikiPage getPage(String title) {
        return pages.get(title);
    }

    public synchronized WikiPage getPage(WikiLink link) {
        return links.get(link);
    }

    public synchronized boolean hasPage(String page) {
        return this.pages.containsKey(page);
    }

    public synchronized boolean hasPage(WikiLink page) {
        return this.links.containsKey(page);
    }

    public synchronized Collection<WikiPage> getPages() {
        return Collections.unmodifiableCollection(pages.values());
    }

    public void calculateAllShortestPaths() {
        try {
            for (WikiPage page : pages.values()) {
                updatePaths(page);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePaths(WikiPage page) {
        Queue<WikiPage> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[pages.size()];
        visited[page.getUniqueId()] = true;
        queue.add(page);

        List<List<WikiPage>> allPairs = page.getAllPairShortest();
        allPairs.clear();

        for(int i=0;i<pages.size();i++) {
            allPairs.add(new ArrayList<>());
        }

        allPairs.get(page.getUniqueId()).add(page);

        int layer = 0;
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
            layer++;
        }
    }
}
