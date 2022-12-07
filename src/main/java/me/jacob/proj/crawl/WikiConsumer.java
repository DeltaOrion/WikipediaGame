package me.jacob.proj.crawl;

import me.jacob.proj.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.crawl.analysis.WikiDocumentAnalyzer;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.Wikipedia;

public class WikiConsumer implements Runnable {

    private final Wikipedia wikipedia;
    private final WikiCrawler crawler;

    public WikiConsumer(Wikipedia wikipedia, WikiCrawler crawler) {
        this.wikipedia = wikipedia;
        this.crawler = crawler;
    }

    @Override
    public void run() {
        //add stop logic
        while(!Thread.currentThread().isInterrupted()) {
            WebDocument document = null;
            DocumentAnalyzer analyzer = new WikiDocumentAnalyzer();
            try {
                document = crawler.nextFetched();
                System.out.println("Consuming "+document.getWikiLink());
                analyzer.setDocument(document);
                analyzer.analyze();
                WikiPage page = analyzer.getPage();
                if(page==null)
                    throw new MalformedPageException();

                System.out.println("Finished Consuming "+page.getTitle());
                crawler.link(page,analyzer.getLinks());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (MalformedPageException e) {
                //if the page is not valid for whatever reason
                if(document!=null)
                    crawler.unlink(document.getWikiLink());

                System.out.println("Invalid Page '"+document.getWikiLink()+"' Reason: "+e.getMessage());
            }
        }
    }
}
