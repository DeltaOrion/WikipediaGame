package me.jacob.proj.crawl;

import me.jacob.proj.crawl.fetch.*;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.Wikipedia;
import me.jacob.proj.util.Poisonable;

import java.nio.file.Path;

public class WikiProducer implements Runnable {

    private final int id;
    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;
    private final DocumentFetcher fetcher;

    public WikiProducer(int id, Wikipedia wikipedia, WikiCrawler crawler, DocumentFetcher fetcher) {
        this.id = id;
        this.wikipedia = wikipedia;
        this.crawler = crawler;
        this.fetcher = fetcher;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Poisonable<WikiLink> taken = crawler.nextLink();
                if(taken.isPoisoned()) {
                    debug("Shutting Down");
                    return;
                }

                WikiLink link = taken.getItem();
                produce(link);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void produce(WikiLink link) throws InterruptedException {
        if(link.isMainPage()) {
            crawler.unlink(link);
            return;
        }

        FetchResult fetched = fetcher.fetch(link);
        switch (fetched.getStatus()) {
            case SUCCESS -> {
                debug("Fetched "+link.getLink());
                crawler.addFetched(fetched);
            } case DOES_NOT_EXIST -> {
                crawler.unlink(link);
            } case CONNECTION_ERROR -> {
                debug("Connection Error when fetching '"+link.getLink()+"'");
                crawler.stash(link);
            } default -> {
                throw new IllegalStateException();
            }
        }
    }


    private void debug(String line) {
        System.out.println("[Producer "+id+"] "+line);
    }
}
