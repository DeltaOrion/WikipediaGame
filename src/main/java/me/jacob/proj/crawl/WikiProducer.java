package me.jacob.proj.crawl;

import me.jacob.proj.crawl.fetch.DocumentFetcher;
import me.jacob.proj.crawl.fetch.TestDocumentFetcher;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.Wikipedia;

public class WikiProducer implements Runnable {

    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;

    public WikiProducer(Wikipedia wikipedia, WikiCrawler crawler) {
        this.wikipedia = wikipedia;
        this.crawler = crawler;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WikiLink link = crawler.nextLink();
                produce(link);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void produce(WikiLink link) throws InterruptedException {
        DocumentFetcher fetcher = new TestDocumentFetcher();
        if(wikipedia.hasPage(link))
            return;

        WebDocument fetched = fetcher.fetch(link.getLink());
        if(fetched == null) {
            crawler.unlink(link);
        } else {
            crawler.addFetched(fetched);
        }
    }
}
