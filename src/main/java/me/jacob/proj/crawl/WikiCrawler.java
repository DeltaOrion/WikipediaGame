package me.jacob.proj.crawl;

import me.jacob.proj.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.crawl.analysis.TestAnalyzer;
import me.jacob.proj.crawl.analysis.WikiDocumentAnalyzer;
import me.jacob.proj.crawl.fetch.DocumentFetcher;
import me.jacob.proj.crawl.fetch.TestDocumentFetcher;
import me.jacob.proj.crawl.fetch.WebDocumentFetcher;
import me.jacob.proj.model.*;
import me.jacob.proj.util.Poisonable;
import me.jacob.proj.util.TestPage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
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

    private final DocumentAnalyzer analyzer;
    private final DocumentFetcher fetcher;

    public static void main(String[] args) throws IOException, InterruptedException {
        Wikipedia wikipedia = new Wikipedia();
        LinkRepository repository = new LinkRepository();

        TestDocumentFetcher fetcher = getTest1();

        WikiCrawler crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setConsumers(1)
                .setProducers(1)
                .setAnalyzer(new TestAnalyzer())
                .setFetcher(fetcher)
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        crawler.await();

        TestPage page = fetcher.getPage("1");
        page.setTitle("Title1");
        page.setDescription("description");
        page.removeLink("3");

        WikiPage wikiPage = wikipedia.getPage(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        System.out.println("--- Details ---");
        System.out.println(wikiPage.getTitle());
        System.out.println(wikiPage.getDescription());
        System.out.println(wikiPage.getNeighbours());
        System.out.println("----------------");

        crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setConsumers(1)
                .setProducers(1)
                .setAnalyzer(new TestAnalyzer())
                .setFetcher(fetcher)
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        crawler.await();

        wikiPage = wikipedia.getPage(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        System.out.println("--- Details ---");
        System.out.println(wikiPage.getTitle());
        System.out.println(wikiPage.getDescription());
        System.out.println(wikiPage.getNeighbours());
        System.out.println("----------------");
    }

    private static TestDocumentFetcher getTest1() throws IOException {
        TestDocumentFetcher fetcher = new TestDocumentFetcher();
        File directory = new File("testpages").toPath().resolve("b").toFile();
        for(File file : directory.listFiles()) {
            if(file.getName().endsWith(".txt")) {
                fetcher.addPage(TestPage.fromFile(file));
            }
        }

        return fetcher;
    }

    private WikiCrawler(Builder builder) {
        this.wikipedia = builder.wikipedia;
        this.repository = builder.repository;

        this.producers = builder.producers;
        this.consumers = builder.consumers;

        this.earlyStop = builder.earlyStop;

        this.shutDownOnEarlyStop = builder.shutDownOnEarlyStop;
        this.shutdownOnSize = builder.shutDownOnSize;

        this.fetcher = builder.fetcher;
        this.analyzer = builder.analyzer;

        indexed = 0;
        this.isShutDown = false;
        this.awaitLatch = new CountDownLatch(1);

        this.executors = Executors.newFixedThreadPool(producers + consumers);
        this.urls = new LinkedBlockingDeque<>();
        this.fetched = new ArrayBlockingQueue<>(builder.documentMaxCapacity);
    }

    public void start(WikiLink startURL) throws InterruptedException {
        if(isShutDown)
            throw new IllegalStateException("Wiki Crawler has shut down");

        size = 1;
        for (int i = 0; i < consumers; i++)
            this.executors.submit(new WikiConsumer(i,wikipedia, this, analyzer));

        for (int i = 0; i < producers; i++) {
            this.executors.submit(new WikiProducer(i, wikipedia, this, fetcher));
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
        if (shutdownOnSize && size == 0) {
            debug("Shutting down - no more pages");
            shutdown();
        }
    }

    public void shutdown() {
        if(isShutDown)
            return;

        isShutDown = true;
        debug("Shutting down");
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

    public void create(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        wikipedia.addPage(page);
        link(page,links);
    }

    public void update(WikiPage wikiPage, WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        UpdateStatus status = wikiPage.update(page,links);
        wikipedia.update(wikiPage,status);
        if(status.isUpdateLinks()) {
            wikiPage.clearNeighbours();
            link(wikiPage,links);
        }
    }

    private void link(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
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
        unconnected = pageRegLink.getUnconnected();
        pageRegLink.unlink();

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

        if (shutDownOnEarlyStop && earlyStop >= 0 && indexed >= earlyStop) {
            debug("Shutting down: max pages indexed");
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

    private void debug(String line) {
        System.out.println("[Crawler] "+line);
    }

    public static class Builder {
        private final Wikipedia wikipedia;
        private final LinkRepository repository;
        private int producers;
        private int consumers;

        private int earlyStop;
        private boolean shutDownOnEarlyStop;
        private boolean shutDownOnSize;

        private DocumentFetcher fetcher;
        private DocumentAnalyzer analyzer;

        private int documentMaxCapacity;

        public Builder(Wikipedia wikipedia, LinkRepository repository) {
            this.wikipedia = wikipedia;
            this.repository = repository;

            this.producers = 1;
            this.consumers = 1;
            this.earlyStop = -1;

            this.shutDownOnEarlyStop = false;
            this.shutDownOnSize = false;
            this.documentMaxCapacity = 1000;

            this.fetcher = new WebDocumentFetcher();
            this.analyzer = new WikiDocumentAnalyzer();
        }

        public Wikipedia getWikipedia() {
            return wikipedia;
        }

        public LinkRepository getRepository() {
            return repository;
        }

        public int getProducers() {
            return producers;
        }

        public Builder setProducers(int producers) {
            this.producers = producers;
            return this;
        }

        public int getConsumers() {
            return consumers;
        }

        public Builder setConsumers(int consumers) {
            this.consumers = consumers;
            return this;
        }

        public int getEarlyStop() {
            return earlyStop;
        }

        public Builder setEarlyStop(int earlyStop) {
            this.earlyStop = earlyStop;
            return this;
        }

        public boolean isShutDownOnEarlyStop() {
            return shutDownOnEarlyStop;
        }

        public Builder setShutDownOnEarlyStop(boolean shutDownOnEarlyStop) {
            this.shutDownOnEarlyStop = shutDownOnEarlyStop;
            return this;
        }

        public DocumentFetcher getFetcher() {
            return fetcher;
        }

        public Builder setFetcher(DocumentFetcher fetcher) {
            this.fetcher = fetcher;
            return this;
        }

        public DocumentAnalyzer getAnalyzer() {
            return analyzer;
        }

        public Builder setAnalyzer(DocumentAnalyzer analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        public boolean isShutDownOnSize() {
            return shutDownOnSize;
        }

        public Builder setShutDownOnSize(boolean shutDownOnSize) {
            this.shutDownOnSize = shutDownOnSize;
            return this;
        }

        public int getDocumentMaxCapacity() {
            return documentMaxCapacity;
        }

        public Builder setDocumentMaxCapacity(int documentMaxCapacity) {
            this.documentMaxCapacity = documentMaxCapacity;
            return this;
        }

        public WikiCrawler build() {
            return new WikiCrawler(this);
        }
    }

}
