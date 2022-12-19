package me.jacob.proj.crawl;

import me.jacob.proj.crawl.analysis.*;
import me.jacob.proj.model.UpdateStatus;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.Wikipedia;
import me.jacob.proj.util.Poisonable;

public class WikiConsumer implements Runnable {

    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;
    private final int id;
    private final AnalyzerType analyzerType;

    public WikiConsumer(int id, Wikipedia wikipedia, WikiCrawler crawler,AnalyzerType analyzerType) {
        this.wikipedia = wikipedia;
        this.crawler = crawler;
        this.id = id;
        this.analyzerType = analyzerType;
    }

    @Override
    public void run() {
        //add stop logic
        while(!Thread.currentThread().isInterrupted()) {
            FetchResult document = null;
            DocumentAnalyzer analyzer = createAnalyzer();
            try {
                Poisonable<FetchResult> taken = crawler.nextFetched();
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

                //we successfully found and analyzed the analyzed.
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
                System.out.println("Invalid Page '"+document.getWikiLink()+"' Reason: "+e.getMessage());
            } catch (Throwable e) {
                //in this case an error occurred with the analyzer. Stash the link for further
                //usage and log the error.
                if(document!=null)
                    crawler.stash(document.getWikiLink());

                e.printStackTrace();
            }
        }
    }

    private DocumentAnalyzer createAnalyzer() {
        switch (this.analyzerType) {
            case WIKIPEDIA -> {
                return new WikiDocumentAnalyzer();
            }
            case TEST -> {
                return new TestAnalyzer();
            }
            case REDIRECT -> {
                return new RedirectDocumentAnalyzer();
            }
            default -> throw new IllegalStateException();
        }
    }

    private void debug(String line) {
        System.out.println("[Consumer "+id+"] "+line);
    }
}
