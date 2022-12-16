package me.jacob.proj.crawl;

import me.jacob.proj.crawl.fetch.FetcherType;
import me.jacob.proj.model.*;
import me.jacob.proj.util.Poisonable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class WikiCrawler {

    private final BlockingDeque<Poisonable<WikiLink>> urls;
    private final BlockingQueue<Poisonable<FetchResult>> fetched;
    private final Wikipedia wikipedia;

    //improve processed repository logic.
    private final LinkRepository repository;
    private int size = 0;

    private final int producers;
    private final int consumers;

    private final int earlyStop;
    private int indexed;

    private boolean shutdownOnSize;
    private boolean shutDownOnEarlyStop;

    private boolean isShutDown;
    private final CountDownLatch awaitLatch;

    private ExecutorService executors;

    public static void main(String[] args) throws IOException, InterruptedException {
        Wikipedia wikipedia = new Wikipedia();
        LinkRepository repository = new LinkRepository();
        WikiCrawler crawler = new WikiCrawler(wikipedia, repository, 1, 1, 50,true,true);
        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
        crawler.await();
        Files.move(FileSystems.getDefault().getPath("testpages").resolve("Black_hole.txt"), FileSystems.getDefault().getPath("testpages").resolve("t").resolve("Black_hole.txt"));
        crawler = new WikiCrawler(wikipedia, repository, 1, 1, 50,true,true);
        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
        crawler.await();
        Files.move(FileSystems.getDefault().getPath("testpages").resolve("t").resolve("Black_hole.txt"), FileSystems.getDefault().getPath("testpages").resolve("Black_hole.txt"));
    }

    public WikiCrawler(Wikipedia wikipedia, LinkRepository repository, int producers, int consumers, int earlystop, boolean shutdownOnSize, boolean shutDownOnEarlyStop) {
        this.wikipedia = wikipedia;
        this.repository = repository;

        this.urls = new LinkedBlockingDeque<>();
        this.fetched = new ArrayBlockingQueue<>(1000);

        this.producers = producers;
        this.consumers = consumers;

        this.executors = Executors.newFixedThreadPool(producers + consumers);
        this.earlyStop = earlystop;

        this.shutDownOnEarlyStop = shutDownOnEarlyStop;
        this.shutdownOnSize = shutdownOnSize;

        indexed = 0;
        this.isShutDown = false;
        this.awaitLatch = new CountDownLatch(1);
    }

    public void start(WikiLink startURL) throws InterruptedException {
        if(isShutDown)
            throw new IllegalStateException("Wiki Crawler has shut down");

        size = 1;
        for (int i = 0; i < consumers; i++)
            this.executors.submit(new WikiConsumer(wikipedia, this));

        for (int i = 0; i < producers; i++) {
            this.executors.submit(new WikiProducer(wikipedia, this, FetcherType.TEST));
        }

        putLink(startURL);
    }

    public Poisonable<WikiLink> nextLink() throws InterruptedException {
        return urls.take();
    }

    public void addFetched(FetchResult document) throws InterruptedException {
        fetched.add(Poisonable.item(document));
    }

    public Poisonable<FetchResult> nextFetched() throws InterruptedException {
        return fetched.take();
    }

    public void unlink(WikiLink link) {
        //for whatever reason the link couldn't be fetched (malformed or non-existent).
        repository.deregister(link);

        //if we have already found a page, then remove it
        WikiPage page = wikipedia.getPage(link);
        if(page!=null)
            page.setRemoved(true);

        shrinkSize();
    }

    public void stash(WikiLink link) {
        repository.stash(link);
        shrinkSize();
    }

    private synchronized void shrinkSize() {
        size--;
        if (shutDownOnEarlyStop && size == 0) {
            System.out.println("Shutting down - no more pages");
            shutdown();
        }
    }

    public void shutdown() {
        if(isShutDown)
            return;

        isShutDown = true;
        System.out.println("Shutting down");
        executors.shutdown();
        stopWorkers();


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

        awaitLatch.countDown();
    }

    private void await() throws InterruptedException {
        awaitLatch.await();
    }

    private void stopWorkers() {
        for (int i = 0; i < consumers; i++)
            fetched.add(Poisonable.poison());

        for (int i = 0; i < producers; i++)
            urls.push(Poisonable.poison());
    }

    public void link(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        wikipedia.addPage(page);
        CrawlableLink pageRegLink = repository.getOrMake(page.getLink());
        pageRegLink.setProcessed();

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

        for (WikiLink link : unindexed) {
            CrawlableLink registered = repository.getOrMake(link);
            registered.addUnconnected(page);
            addLink(registered);
        }

        //the unconnected link to this page. Add the links!
        Collection<WikiPage> unconnected = null;
        synchronized (this) {
            unconnected = pageRegLink.getUnconnected();
            pageRegLink.unlink();
        }

        if (unconnected != null) {
            for (WikiPage p : unconnected) {
                p.addNeighbour(page);
            }
        }

        synchronized (this) {
            shrinkSize();
            incrementPages();
        }
    }

    private void incrementPages() {
        indexed++;

        if (shutDownOnEarlyStop && indexed >= earlyStop) {
            System.out.println("Shutting down - max pages indexed");
            shutdown();
        }
    }

    //adds an indexed link
    private synchronized void addLink(CrawlableLink link) throws InterruptedException {
        if (!repository.shouldBeCrawled(link))
            return;

        link.setRegistered(true);
        size++;

        putLink(link.getLink());
    }

    private void putLink(WikiLink link) throws InterruptedException {
        urls.put(Poisonable.item(link));
    }

}
