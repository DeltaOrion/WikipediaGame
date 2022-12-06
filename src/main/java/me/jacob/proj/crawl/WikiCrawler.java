package me.jacob.proj.crawl;

import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.Wikipedia;

import java.net.MalformedURLException;
import java.net.SocketImpl;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikiCrawler {

    private final BlockingQueue<WikiLink> urls;
    private final BlockingQueue<WebDocument> fetched;
    private final Wikipedia wikipedia;

    private final Set<WikiLink> currentlyProcessed;

    private final Map<WikiLink, List<WikiPage>> unconnectedEdges;
    private int size = 0;

    private final int producers;
    private final int consumers;

    private ExecutorService executors;

    public static void main(String[] args) throws MalformedURLException {
        Wikipedia wikipedia = new Wikipedia();
        WikiCrawler crawler = new WikiCrawler(wikipedia, 1, 1);
        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
    }


    public WikiCrawler(Wikipedia wikipedia, int producers, int consumers) {
        this.wikipedia = wikipedia;
        this.urls = new ArrayBlockingQueue<>(10000);
        this.fetched = new ArrayBlockingQueue<>(1000);
        this.unconnectedEdges = new HashMap<>();

        this.producers = producers;
        this.consumers = consumers;

        this.executors = Executors.newFixedThreadPool(producers + consumers);
        this.currentlyProcessed = new HashSet<>();
    }

    public void start(WikiLink startURL) {
        size = 1;
        for (int i = 0; i < consumers; i++)
            this.executors.submit(new WikiConsumer(wikipedia, this));

        for (int i = 0; i < producers; i++) {
            this.executors.submit(new WikiProducer(wikipedia, this));
        }

        urls.add(startURL);
    }

    public WikiLink nextLink() throws InterruptedException {
        return urls.take();
    }

    public void addFetched(WebDocument document) throws InterruptedException {
        fetched.add(document);
    }

    public WebDocument nextFetched() throws InterruptedException {
        return fetched.take();
    }

    public synchronized void unlink(WikiLink link) {
        //for whatever reason the link couldn't be fetched (malformed or non-existant).
        unconnectedEdges.remove(link);
        size--;
        if (size == 0)
            terminate();
    }

    private void terminate() {
        System.out.println("Terminating");
        executors.shutdownNow();

        for (WikiPage page : wikipedia.getPages()) {
            System.out.println(page.getTitle() + " " + page.getUniqueId());
            for (WikiPage neighbour : page.getNeighbours()) {
                System.out.println(" - " + neighbour.getTitle());
            }
        }

        try {
            wikipedia.calculateAllShortestPaths();
            System.out.println(wikipedia.getShortestPaths("Black hole", "Ultra-high-energy cosmic ray"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void link(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        wikipedia.addPage(page);
        size--;
        for (WikiLink link : links) {
            WikiPage pageLink = wikipedia.getPage(link);
            if (pageLink != null) {
                page.addNeighbour(pageLink);
            } else {
                //in this case the link has not been crawled yet
                //we need to add it to the unconnectedEdges
                synchronized (this) {
                    List<WikiPage> unconnected = unconnectedEdges.computeIfAbsent(link, k -> new ArrayList<>());
                    unconnected.add(page);
                }
                //add the link to be crawled
                addLink(link);
            }
        }

        //the unconnected link to this page. Add the links!
        List<WikiPage> unconnected = unconnectedEdges.get(page.getLink());
        if (unconnected != null) {
            for (WikiPage p : unconnected)
                p.addNeighbour(page);

            synchronized (this) {
                unconnectedEdges.remove(page.getLink());
            }
        }

        if (size == 0)
            terminate();
    }

    private void addLink(WikiLink link) throws InterruptedException {
        synchronized (this) {
            if (currentlyProcessed.contains(link))
                return;

            currentlyProcessed.add(link);
            size++;
        }
        urls.put(link);
    }
}
