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
    private boolean running;

    //-- Stats Divider --//
    private int fetched = 0;
    private long totalStarvationTime = 0;

    private int placed = 0;
    private long totalBlockedTime = 0;

    public WikiProducer(int id, Wikipedia wikipedia, WikiCrawler crawler, DocumentFetcher fetcher) {
        this.id = id;
        this.wikipedia = wikipedia;
        this.crawler = crawler;
        this.fetcher = fetcher;
        this.running = false;
    }

    @Override
    public void run() {
        synchronized (this) {
            if(running)
                return;

            running = true;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                long beforeTime = System.nanoTime();
                Poisonable<WikiLink> taken = crawler.nextLink();
                long afterTime = System.nanoTime();

                synchronized (this) {
                    totalStarvationTime += (afterTime - beforeTime);
                    fetched++;
                }

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
                long before = System.nanoTime();
                crawler.addFetched(fetched);
                long after = System.nanoTime();

                synchronized (this) {
                    totalBlockedTime += (after - before);
                    placed++;
                }
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

    public synchronized int getPlaced() {
        return placed;
    }

    public synchronized long getTotalBlockedTime() {
        return totalBlockedTime;
    }

    public synchronized int getFetched() {
        return fetched;
    }

    public synchronized long getTotalStarvationTime() {
        return totalStarvationTime;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public int getId() {
        return id;
    }
}
