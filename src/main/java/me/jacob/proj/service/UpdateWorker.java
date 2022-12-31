package me.jacob.proj.service;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.service.crawl.WikiCrawler;

public class UpdateWorker implements Runnable {

    private final int id;
    private final LinkService service;
    private final WikiCrawler crawler;

    public UpdateWorker(int id, LinkService service, WikiCrawler crawler) {
        this.id = id;
        this.service = service;
        this.crawler = crawler;
    }

    @Override
    public void run() {
        debug("Starting");
        while(!Thread.currentThread().isInterrupted() && !crawler.isShutDown()) {
            for(CrawlableLink link : service.getNextBlock()) {
                if(service.shouldBeCrawled(link)) {
                    debug("Pushing link to be updated '"+link+"'");
                    crawler.addURL(link.getLink());
                }
            }
        }
    }

    private void debug(String line) {
        System.out.println("[Updater "+id+"] "+line);
    }
}
