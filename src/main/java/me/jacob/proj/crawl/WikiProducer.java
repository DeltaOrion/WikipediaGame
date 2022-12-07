package me.jacob.proj.crawl;

import me.jacob.proj.crawl.fetch.DocumentFetcher;
import me.jacob.proj.crawl.fetch.FetcherType;
import me.jacob.proj.crawl.fetch.TestDocumentFetcher;
import me.jacob.proj.crawl.fetch.WebDocumentFetcher;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.Wikipedia;
import me.jacob.proj.util.Poisonable;

public class WikiProducer implements Runnable {

    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;
    private final DocumentFetcher fetcher;

    public WikiProducer(Wikipedia wikipedia, WikiCrawler crawler, FetcherType type) {
        this.wikipedia = wikipedia;
        this.crawler = crawler;
        this.fetcher = getFetcher(type);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Poisonable<WikiLink> taken = crawler.nextLink();
                if(taken.isPoisoned()) {
                    System.out.println("Poison shutting down");
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
        if(wikipedia.hasPage(link))
            return;

        if(link.isMainPage()) {
            crawler.unlink(link);
            return;
        }

        WebDocument fetched = fetcher.fetch(link.getLink());
        if(fetched == null) {
            crawler.unlink(link);
        } else {
            System.out.println("Fetched "+link.getLink());
            crawler.addFetched(fetched);
        }
    }

    private DocumentFetcher getFetcher(FetcherType type) {
        switch (type) {
            case WEB -> {
                return new WebDocumentFetcher();
            }
            case TEST -> {
                return new TestDocumentFetcher();
            }
        }

        throw new UnsupportedOperationException();
    }
}
