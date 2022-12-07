package me.jacob.proj.crawl;

import me.jacob.proj.crawl.fetch.FetcherType;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.Wikipedia;
import me.jacob.proj.util.Poisonable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class WikiCrawler {

    private final BlockingDeque<Poisonable<WikiLink>> urls;
    private final BlockingQueue<Poisonable<WebDocument>> fetched;
    private final Wikipedia wikipedia;

    private final Set<WikiLink> currentlyProcessed;

    private final Map<WikiLink, List<WikiPage>> unconnectedEdges;
    private int size = 0;

    private final int producers;
    private final int consumers;

    private final int earlyStop;
    private int indexed;

    private ExecutorService executors;

    public static void main(String[] args) throws MalformedURLException, InterruptedException {
        Wikipedia wikipedia = new Wikipedia();
        WikiCrawler crawler = new WikiCrawler(wikipedia, 2, 5,20);
        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
    }


    public WikiCrawler(Wikipedia wikipedia, int producers, int consumers, int earlystop) {
        this.wikipedia = wikipedia;
        this.urls = new LinkedBlockingDeque<>();
        this.fetched = new ArrayBlockingQueue<>(1000);
        this.unconnectedEdges = new HashMap<>();

        this.producers = producers;
        this.consumers = consumers;

        this.executors = Executors.newFixedThreadPool(producers + consumers);
        this.currentlyProcessed = new HashSet<>();

        this.earlyStop = earlystop;
        indexed = 0;
    }

    public void start(WikiLink startURL) throws InterruptedException {
        size = 1;
        for (int i = 0; i < consumers; i++)
            this.executors.submit(new WikiConsumer(wikipedia, this));

        for (int i = 0; i < producers; i++) {
            this.executors.submit(new WikiProducer(wikipedia, this, FetcherType.WEB));
        }

        putLink(startURL);
    }

    public Poisonable<WikiLink> nextLink() throws InterruptedException {
        return urls.take();
    }

    public void addFetched(WebDocument document) throws InterruptedException {
        fetched.add(Poisonable.item(document));
    }

    public Poisonable<WebDocument> nextFetched() throws InterruptedException {
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

        executors.shutdown();
        shutdown();

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

    private void shutdown() {
        for(int i=0;i<consumers;i++)
            fetched.add(Poisonable.poison());

        for(int i=0;i<producers;i++)
            urls.push(Poisonable.poison());
    }

    public void link(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        wikipedia.addPage(page);

        List<WikiLink> unindexed = new ArrayList<>();
        for (WikiLink link : links) {
            WikiPage pageLink = wikipedia.getPage(link);
            if (pageLink != null) {
                page.addNeighbour(pageLink);
            } else {
                //in this case the link has not been crawled yet
                //we need to add it to the unconnectedEdged
                //add the link to be crawled
                unindexed.add(link);
            }
        }

        synchronized (this) {
            for(WikiLink link : unindexed) {
                List<WikiPage> unconnected = unconnectedEdges.computeIfAbsent(link, k -> new ArrayList<>());
                unconnected.add(page);
                addLink(link);
            }
        }

        //the unconnected link to this page. Add the links!
        List<WikiPage> unconnected = null;
        synchronized (this) {
            unconnected = unconnectedEdges.get(page.getLink());
            unconnectedEdges.remove(page.getLink());
        }

        if (unconnected != null) {
            for (WikiPage p : unconnected) {
                p.addNeighbour(page);
            }
        }

        synchronized (this) {
            size--;
            indexed++;

            if (size == 0) {
                terminate();
                return;
            }

            if(indexed >= earlyStop) {
                terminate();
                return;
            }
        }
    }

    private void addLink(WikiLink link) throws InterruptedException {
        synchronized (this) {
            if (currentlyProcessed.contains(link))
                return;

            currentlyProcessed.add(link);
            size++;
        }
        putLink(link);
    }

    private void putLink(WikiLink link) throws InterruptedException {
        urls.put(Poisonable.item(link));
    }
}
