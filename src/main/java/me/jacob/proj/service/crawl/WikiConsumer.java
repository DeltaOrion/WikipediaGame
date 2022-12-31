package me.jacob.proj.service.crawl;

import me.jacob.proj.service.crawl.analysis.*;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.util.Poisonable;

public class WikiConsumer implements Runnable {

    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;
    private final int id;
    private final DocumentAnalyzer analyzer;
    private boolean running;

    //--- Stats Divider --/
    private int consumed = 0;
    private long totalStarvationTime = 0;

    public WikiConsumer(int id, Wikipedia wikipedia, WikiCrawler crawler, DocumentAnalyzer analyzer) {
        this.wikipedia = wikipedia;
        this.crawler = crawler;
        this.id = id;
        this.analyzer = analyzer;
        this.running = false;
    }

    @Override
    public void run() {
        synchronized (this) {
            if(running)
                return;

            running = true;
        }
        //add stop logic
        while(!Thread.currentThread().isInterrupted()) {
            FetchResult document = null;
            try {
                long beforeTimer = System.nanoTime();
                Poisonable<FetchResult> taken = crawler.nextFetched();
                long afterTimer = System.nanoTime();

                synchronized (this) {
                    consumed++;
                    totalStarvationTime += (afterTimer - beforeTimer);
                }

                if(taken.isPoisoned()) {
                    debug("Shutting Down");
                    return;
                }

                document = taken.getItem();
                debug("Consuming "+ document.getWikiLink());
                analyzer.setDocument(document);
                analyzer.analyze();
                WikiPage analyzed = analyzer.getPage();
                if(analyzed==null)
                    throw new MalformedPageException();

                //we successfully found and analyzed the page.
                analyzed.setRemoved(false);
                debug("Finished Consuming "+document.getWikiLink());

                WikiPage wikiPage = wikipedia.getPage(document.getWikiLink());
                if(wikiPage!=null) {
                    debug("Updating "+wikiPage.getTitle());
                    crawler.update(wikiPage,analyzed,analyzer.getLinks());
                } else {
                    debug("Creating "+analyzed.getTitle());
                    crawler.create(analyzed, analyzer.getLinks());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (MalformedPageException e) {
                //if the page is not valid for whatever reason
                crawler.unlink(document.getWikiLink());
                debug("Invalid Page '"+document.getWikiLink()+"' Reason: "+e.getMessage());
            } catch (Throwable e) {
                //in this case an error occurred with the analyzer. Stash the link for further
                //usage and log the error.
                if(document!=null)
                    crawler.stash(document.getWikiLink());

                e.printStackTrace();
            }
        }
    }

    private void debug(String line) {
        System.out.println("[Consumer "+id+"] "+line);
    }

    public synchronized int getConsumed() {
        return consumed;
    }

    public synchronized long getTotalStarvationTime() {
        return totalStarvationTime;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public int getId() {
        return this.id;
    }
}
