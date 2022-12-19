package me.jacob.proj.crawl;

import me.jacob.proj.crawl.analysis.AnalyzerType;
import me.jacob.proj.crawl.fetch.FetcherType;
import me.jacob.proj.model.*;
import me.jacob.proj.util.Poisonable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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

    private final AnalyzerType analyzerType;
    private final FetcherType fetchType;
    private final Path crawlDirectory;

    public static void main(String[] args) throws IOException, InterruptedException {
        Wikipedia wikipedia = new Wikipedia();
        LinkRepository repository = new LinkRepository();
        WikiCrawler crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setConsumers(1)
                .setProducers(1)
                .setAnalyzerType(AnalyzerType.WIKIPEDIA)
                .setFetchType(FetcherType.TEST)
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
        crawler.await();
    }

    private WikiCrawler(Builder builder) {
        this.wikipedia = builder.wikipedia;
        this.repository = builder.repository;

        this.producers = builder.producers;
        this.consumers = builder.consumers;

        this.earlyStop = builder.earlyStop;

        this.shutDownOnEarlyStop = builder.shutDownOnEarlyStop;
        this.shutdownOnSize = builder.shutDownOnSize;

        this.fetchType = builder.fetchType;
        this.analyzerType = builder.analyzerType;
        this.crawlDirectory = builder.crawlDirectory;

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
            this.executors.submit(new WikiConsumer(i,wikipedia, this, analyzerType));

        for (int i = 0; i < producers; i++) {
            this.executors.submit(new WikiProducer(i, wikipedia, this,crawlDirectory, fetchType));
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
        private boolean isShutDownOnEarlyStop;

        private FetcherType fetchType;
        private AnalyzerType analyzerType;
        private Path crawlDirectory;

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

            this.fetchType = FetcherType.WEB;
            this.analyzerType = AnalyzerType.WIKIPEDIA;
            this.crawlDirectory = FileSystems.getDefault().getPath("testpages");
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

        public FetcherType getFetchType() {
            return fetchType;
        }

        public Builder setFetchType(FetcherType fetchType) {
            this.fetchType = fetchType;
            return this;
        }

        public AnalyzerType getAnalyzerType() {
            return analyzerType;
        }

        public Builder setAnalyzerType(AnalyzerType analyzerType) {
            this.analyzerType = analyzerType;
            return this;
        }

        public Path getCrawlDirectory() {
            return crawlDirectory;
        }

        public Builder setCrawlDirectory(Path crawlDirectory) {
            this.crawlDirectory = crawlDirectory;
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
