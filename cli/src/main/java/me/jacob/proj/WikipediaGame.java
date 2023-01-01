package me.jacob.proj;

import me.jacob.proj.model.map.HashMapLinkRepository;
import me.jacob.proj.model.map.HashMapPageRepository;
import me.jacob.proj.service.LinkService;
import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.service.crawl.WikiCrawler;
import me.jacob.proj.service.crawl.analysis.factory.WikiAnalyzerFactory;
import me.jacob.proj.service.crawl.fetch.FileDocumentFetcher;
import me.jacob.proj.util.AtomicIntCounter;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class WikipediaGame {

    private WikiCrawler crawler;
    private Wikipedia pageService;
    private LinkService linkService;

    public WikipediaGame() {
        linkService = new LinkService(new HashMapLinkRepository(new AtomicIntCounter()), 100);
        linkService.setTimeBetweenUpdates(Duration.of(30, ChronoUnit.SECONDS));
        pageService = new Wikipedia(linkService, new HashMapPageRepository(new AtomicIntCounter()));

        crawler = new WikiCrawler.Builder(pageService,linkService)
                .setShutDownOnEarlyStop(true)
                .setShutDownOnSize(false)
                .setConsumers(1)
                .setProducers(1)
                .setCreatesUntilCalculation(10)
                .setUpdatesUntilCalculation(10)
                .setAnalyzer(new WikiAnalyzerFactory())
                .setFetcher(new FileDocumentFetcher(new File("testpages").toPath()))
                .build();
    }

    public void shutdown() {
        crawler.shutdown();
        pageService.publishBulkCreate();
    }

    public WikiCrawler getCrawler() {
        return crawler;
    }

    public Wikipedia getPageService() {
        return pageService;
    }

    public LinkService getLinkService() {
        return linkService;
    }

    public void setCrawler(WikiCrawler crawler) {
        this.crawler = crawler;
    }

    public void setPageService(Wikipedia pageService) {
        this.pageService = pageService;
    }

    public void setLinkService(LinkService linkService) {
        this.linkService = linkService;
    }
}
