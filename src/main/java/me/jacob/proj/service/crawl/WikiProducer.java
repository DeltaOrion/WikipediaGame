package me.jacob.proj.service.crawl;

import me.jacob.proj.service.crawl.fetch.*;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.util.Poisonable;

public class WikiProducer implements Runnable {

    private final int id;
    private final WikiCrawler crawler;
    private final DocumentFetcher fetcher;
    private boolean running;

    //-- Stats Divider --//
    private int fetched = 0;
    private long totalStarvationTime = 0;

    private int placed = 0;
    private long totalBlockedTime = 0;

    public WikiProducer(int id, WikiCrawler crawler, DocumentFetcher fetcher) {
        this.id = id;
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
        FetchResult fetched = fetcher.fetch(link);
        long before = System.nanoTime();
        crawler.addFetched(fetched);
        long after = System.nanoTime();

        synchronized (this) {
            totalBlockedTime += (after - before);
            placed++;
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
