package me.jacob.proj.service.crawl;

import me.jacob.proj.model.page.HashMapLinkRepository;
import me.jacob.proj.model.page.HashMapPageRepository;
import me.jacob.proj.service.crawl.analysis.factory.AnalyzerFactory;
import me.jacob.proj.service.crawl.analysis.factory.TestAnalyzerFactory;
import me.jacob.proj.service.crawl.analysis.factory.WikiAnalyzerFactory;
import me.jacob.proj.service.crawl.fetch.DocumentFetcher;
import me.jacob.proj.service.crawl.fetch.FileDocumentFetcher;
import me.jacob.proj.service.crawl.fetch.TestDocumentFetcher;
import me.jacob.proj.service.crawl.fetch.WebDocumentFetcher;
import me.jacob.proj.model.*;
import me.jacob.proj.service.LinkService;
import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.util.Poisonable;
import me.jacob.proj.util.TestPage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class WikiCrawler {

    private final BlockingDeque<Poisonable<WikiLink>> urls;
    private final BlockingQueue<Poisonable<FetchResult>> fetched;

    private final List<WikiConsumer> consumers;
    private final List<WikiProducer> producers;

    //improve processed repository logic.
    private final Wikipedia wikipedia;
    private final LinkService linkService;
    private int size = 0;

    private final int noOfProducers;
    private final int noOfConsumers;

    private final int earlyStop;
    private int indexed;

    private final int createsUntilBulkPublish;
    private final int createsUntilCalculation;
    private final int updatesUntilCalculation;

    private int createsSinceLastPublish;
    private int createsSinceLastCalculation;
    private int updatesSinceLastCalculation;
    private final ConcurrentMap<WikiPage,Object> toCalculate;

    private final boolean shutdownOnSize;
    private final boolean shutDownOnEarlyStop;

    private boolean isShutDown;
    private final CountDownLatch awaitLatch;

    private final ExecutorService executors;

    private final AnalyzerFactory analyzer;
    private final DocumentFetcher fetcher;

    public static void main(String[] args) throws IOException, InterruptedException {
        updateTest();
    }

    private static void performanceTest() throws MalformedURLException, InterruptedException {
        LinkService repository = new LinkService(new HashMapLinkRepository());
        Wikipedia wikipedia = new Wikipedia(repository,new HashMapPageRepository());

        WikiCrawler crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setEarlyStop(1000)
                .setConsumers(1)
                .setProducers(60)
                .setAnalyzer(new WikiAnalyzerFactory())
                .setFetcher(new WebDocumentFetcher())
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
        long before = System.currentTimeMillis();
        crawler.await();
        long after = System.currentTimeMillis();
        long division = 1000000;

        String tableFormat = "%-12s %-25s %-25s\n";
        System.out.printf(tableFormat,"Consumer","Total Time Starved","Average Time Starved");
        System.out.println("--------------------------------------------------------------");
        long starvationTotal = 0;
        int consumedTotal = 0;
        for(WikiConsumer consumer : crawler.getConsumers()) {
            starvationTotal += consumer.getTotalStarvationTime();
            consumedTotal += consumer.getConsumed();
            System.out.printf("%-12d %-25d %-25.2f\n",consumer.getId(),consumer.getTotalStarvationTime()/division,((float)consumer.getTotalStarvationTime())/(consumer.getConsumed()*division));
        }

        starvationTotal /= crawler.getConsumers().size();
        consumedTotal /= crawler.getConsumers().size();

        System.out.printf("%-12s %-25d %-25.2f\n","Total",starvationTotal/division,((float)starvationTotal)/(consumedTotal*division));
        System.out.println();
        System.out.printf("%-12s %-25s %-30s %-25s %-30s\n","Producer","Total Time Starved","Average Time Starved","Total Time Blocked","Average Time Blocked");
        System.out.println("--------------------------------------------------------------");
        starvationTotal = 0;
        consumedTotal = 0;
        long blockedTotal = 0;
        int placedTotal = 0;
        for(WikiProducer producer : crawler.getProducers()) {
            starvationTotal += producer.getTotalStarvationTime();
            consumedTotal += producer.getFetched();
            blockedTotal += producer.getTotalBlockedTime();
            placedTotal += producer.getPlaced();

            System.out.printf("%-12d %-25d %-30.2f %-25d %-30.2f\n",producer.getId(),producer.getTotalStarvationTime()/division,
                    ((float)producer.getTotalStarvationTime())/(producer.getFetched()*division),
                    producer.getTotalBlockedTime()/division,
                    ((float)producer.getTotalBlockedTime())/(producer.getPlaced())*division);
        }

        starvationTotal /= crawler.getProducers().size();
        consumedTotal /= crawler.getProducers().size();

        blockedTotal /= crawler.getProducers().size();
        consumedTotal /= crawler.getProducers().size();

        System.out.printf("%-12s %-25d %-30.2f %-25d %-30.2f\n","total",starvationTotal/division,
                ((float)starvationTotal)/(consumedTotal*division),
                blockedTotal/division,
                ((float)blockedTotal)/(placedTotal)*division);

        System.out.println("Total Time Elapsed: "+(after-before));
    }

    private static void fileTest() throws MalformedURLException, InterruptedException {
        LinkService repository = new LinkService(new HashMapLinkRepository());
        Wikipedia wikipedia = new Wikipedia(repository, new HashMapPageRepository());

        WikiCrawler crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setConsumers(1)
                .setProducers(1)
                .setAnalyzer(new WikiAnalyzerFactory())
                .setFetcher(new FileDocumentFetcher(new File("testpages").toPath()))
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole")));
        crawler.await();
        verbose(wikipedia);
    }

    private static void updateTest() throws IOException, InterruptedException {
        LinkService repository = new LinkService(new HashMapLinkRepository());
        Wikipedia wikipedia = new Wikipedia(repository, new HashMapPageRepository());
        repository.setTimeBetweenUpdates(Duration.ZERO);

        TestDocumentFetcher fetcher = getTest1();
        TestPage five = fetcher.getPage("5");
        five.addLink("8");

        WikiCrawler crawler = new WikiCrawler.Builder(wikipedia,repository)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(true)
                .setConsumers(1)
                .setProducers(1)
                .setAnalyzer(new TestAnalyzerFactory())
                .setFetcher(fetcher)
                .setCreatesUntilBulkPublish(3)
                .setCreatesUntilCalculation(3)
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        crawler.await();
        verbose(wikipedia);

        TestPage page = fetcher.getPage("1");
        page.setTitle("Title1");
        page.setDescription("description");
        page.removeLink("3");
        page.addLink("8");

        TestPage eight = new TestPage("8");
        eight.setTitle("8");
        eight.setDescription("eight");
        eight.addLink("9");
        eight.addLink("2");

        fetcher.addPage(eight);

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
                .setAnalyzer(new TestAnalyzerFactory())
                .setFetcher(fetcher)
                .build();

        crawler.start(new WikiLink(new URL("https://en.wikipedia.org/wiki/1")));
        crawler.await();

        verbose(wikipedia);

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

    private static void verbose(Wikipedia wikipedia) {
        for (WikiPage page : wikipedia.getAllPages()) {
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

    private WikiCrawler(Builder builder) {
        this.wikipedia = builder.wikipedia;
        this.linkService = builder.linkService;

        this.noOfProducers = builder.producers;
        this.noOfConsumers = builder.consumers;
        this.consumers = new ArrayList<>();
        this.producers = new ArrayList<>();

        this.updatesUntilCalculation = builder.updatesUntilCalculation;
        this.createsUntilCalculation = builder.createsUntilCalculation;
        this.createsUntilBulkPublish = builder.createsUntilBulkPublish;

        this.toCalculate = new ConcurrentHashMap<>();
        this.createsSinceLastCalculation = 0;
        this.createsSinceLastPublish = 0;
        this.updatesSinceLastCalculation = 0;

        this.earlyStop = builder.earlyStop;

        this.shutDownOnEarlyStop = builder.shutDownOnEarlyStop;
        this.shutdownOnSize = builder.shutDownOnSize;

        this.fetcher = builder.fetcher;
        this.analyzer = builder.analyzer;

        indexed = 0;
        this.isShutDown = false;
        this.awaitLatch = new CountDownLatch(1);

        this.executors = Executors.newFixedThreadPool(noOfProducers + noOfConsumers);
        this.urls = new LinkedBlockingDeque<>();
        this.fetched = new ArrayBlockingQueue<>(builder.documentMaxCapacity);
    }

    public void start(WikiLink startURL) throws InterruptedException {
        if(isShutDown)
            throw new IllegalStateException("Wiki Crawler has shut down");

        size = 1;

        for (int i = 0; i < noOfConsumers; i++) {
            WikiConsumer consumer = new WikiConsumer(i, wikipedia, this, analyzer.get());
            this.consumers.add(consumer);
            this.executors.submit(consumer);
        }

        for (int i = 0; i < noOfProducers; i++) {
            WikiProducer producer = new WikiProducer(i, wikipedia, this, fetcher);
            this.producers.add(producer);
            this.executors.submit(producer);
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
        //for whatever reason the link couldn't be fetched (malformed or non-existent)
        wikipedia.remove(link);

        shrinkSize();
    }

    public void stash(WikiLink link) {
        linkService.stash(link);
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

        wikipedia.publishBulkCreate();
        calculateAll();
        deregisterLinks();

        awaitLatch.countDown();
    }

    private void deregisterLinks() {
        Iterator<Poisonable<WikiLink>> links = urls.iterator();
        while (links.hasNext()) {
            Poisonable<WikiLink> taken = links.next();
            if(!taken.isPoisoned()) {
                CrawlableLink link =  linkService.get(taken.getItem());
                if(link!=null) {
                    link.setRegistered(false);
                }
                links.remove();
            }
        }
    }

    private void calculateAll() {
        Iterator<WikiPage> pages = toCalculate.keySet().iterator();
        while (pages.hasNext()) {
            wikipedia.updatePaths(pages.next());
            pages.remove();
        }
    }

    private void await() throws InterruptedException {
        awaitLatch.await();
    }

    private void stopWorkers() {
        for (int i = 0; i < noOfConsumers; i++)
            fetched.add(Poisonable.poison());

        for (int i = 0; i < noOfProducers; i++)
            urls.push(Poisonable.poison());
    }

    public void update(WikiPage original, WikiPage newPage, Collection<WikiLink> links) throws InterruptedException {
        Collection<WikiLink> l = wikipedia.update(original,
                newPage.getTitle(),
                newPage.getDescription(),
                newPage.isRedirect(),
                newPage.getArticleType(),
                links);

        addLinks(l);
        if(l.size()>0)
            toCalculate.put(original,new Object());

        boolean doCalculation = false;
        synchronized (this) {
            shrinkSize();
            incrementPages();

            updatesSinceLastCalculation++;
            if(updatesSinceLastCalculation >= updatesUntilCalculation) {
                updatesSinceLastCalculation = 0;
                doCalculation = true;
            }
        }

        if(doCalculation)
            calculateAll();
    }

    public void create(WikiPage page, Collection<WikiLink> links) throws InterruptedException {
        addLinks(wikipedia.bulkCreate(page,links));
        toCalculate.put(page, new Object());
        boolean publishUpdate = false;
        boolean doCalculation = false;
        synchronized (this) {
            createsSinceLastPublish++;
            createsSinceLastCalculation++;

            if(createsSinceLastPublish >= createsUntilBulkPublish) {
                createsSinceLastCalculation = 0;
                publishUpdate = true;
            }

            if(createsSinceLastCalculation >= createsUntilCalculation) {
                createsSinceLastCalculation = 0;
                doCalculation = true;
            }

            shrinkSize();
            incrementPages();
        }

        if(doCalculation)
            calculateAll();

        if(publishUpdate)
            wikipedia.publishBulkCreate();
    }



    private void incrementPages() {
        indexed++;

        if (shutDownOnEarlyStop && earlyStop >= 0 && indexed >= earlyStop) {
            debug("Shutting down: max pages indexed");
            shutdown();
        }
    }

    private void addLinks(Collection<WikiLink> links) throws InterruptedException {
        Collection<CrawlableLink> registeredLinks = linkService.getOrMake(links);
        for(CrawlableLink link : registeredLinks) {
            addLink(link);
        }
    }

    //adds an indexed link
    private void addLink(CrawlableLink link) throws InterruptedException {
        if (!linkService.shouldBeCrawled(link))
            return;

        link.setRegistered(true);
        synchronized (this) {
            size++;
        }

        putLink(link.getLink());
    }

    private void putLink(WikiLink link) throws InterruptedException {
        urls.put(Poisonable.item(link));
    }

    private void debug(String line) {
        System.out.println("[Crawler] "+line);
    }

    public List<WikiConsumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    public List<WikiProducer> getProducers() {
        return Collections.unmodifiableList(producers);
    }


    public static class Builder {
        private final Wikipedia wikipedia;
        private final LinkService linkService;
        private int producers;
        private int consumers;

        private int earlyStop;
        private boolean shutDownOnEarlyStop;
        private boolean shutDownOnSize;

        private DocumentFetcher fetcher;
        private AnalyzerFactory analyzer;

        private int documentMaxCapacity;
        private int createsUntilBulkPublish;
        private int createsUntilCalculation;
        private int updatesUntilCalculation;

        public Builder(Wikipedia wikipedia, LinkService linkService) {
            this.wikipedia = wikipedia;
            this.linkService = linkService;

            //recommended ratio -  60:1
            this.producers = 60;
            this.consumers = 1;
            this.earlyStop = -1;

            this.shutDownOnEarlyStop = false;
            this.shutDownOnSize = false;
            this.documentMaxCapacity = 1000;

            this.createsUntilBulkPublish = 1000;
            this.createsUntilCalculation = 10000;
            this.updatesUntilCalculation = 100;

            this.fetcher = new WebDocumentFetcher();
            this.analyzer = new WikiAnalyzerFactory();
        }

        public Wikipedia getWikipedia() {
            return wikipedia;
        }

        public LinkService getLinkService() {
            return linkService;
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

        public AnalyzerFactory getAnalyzer() {
            return analyzer;
        }

        public Builder setAnalyzer(AnalyzerFactory analyzer) {
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

        public int getCreatesUntilBulkPublish() {
            return createsUntilBulkPublish;
        }

        public Builder setCreatesUntilBulkPublish(int createsUntilCreate) {
            this.createsUntilBulkPublish = createsUntilCreate;
            return this;
        }

        public int getCreatesUntilCalculation() {
            return createsUntilCalculation;
        }

        public Builder setCreatesUntilCalculation(int createsUntilCalculation) {
            this.createsUntilCalculation = createsUntilCalculation;
            return this;
        }

        public int getUpdatesUntilCalculation() {
            return updatesUntilCalculation;
        }

        public Builder setUpdatesUntilCalculation(int updatesUntilCalculation) {
            this.updatesUntilCalculation = updatesUntilCalculation;
            return this;
        }

        public WikiCrawler build() {
            return new WikiCrawler(this);
        }
    }

}
