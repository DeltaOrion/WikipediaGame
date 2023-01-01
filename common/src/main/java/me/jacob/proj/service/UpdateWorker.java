package me.jacob.proj.service;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.service.crawl.WikiCrawler;

import java.util.logging.Logger;

public class UpdateWorker implements Runnable {

    private final int id;
    private final LinkService service;
    private final WikiCrawler crawler;
    private boolean running;

    public UpdateWorker(int id, LinkService service, WikiCrawler crawler) {
        this.id = id;
        this.service = service;
        this.crawler = crawler;
        this.running = true;
    }

    @Override
    public void run() {
        debug("Starting");
        while(!Thread.currentThread().isInterrupted() && running) {
            for(CrawlableLink link : service.getNextBlock()) {
                if(!running)
                    return;

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

    public void stop() {
        running = false;
        debug("Shutting Down");
    }
}
