package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestAnalyzer implements DocumentAnalyzer {

    private FetchResult document;
    private Set<WikiLink> linksFound = new HashSet<>();
    private WikiPage analyzed = null;

    @Override
    public void setDocument(FetchResult document) {
       this.document = document;
    }

    @Override
    public FetchResult getDocument() {
        return document;
    }

    @Override
    public void analyze() throws MalformedPageException {
        Document html = document.getDocument();
        Element body = html.body();
        String title = body.getElementById("title").text();
        Elements links = html.getElementsByTag("a");
        for(Element link : links) {
            linksFound.add(new WikiLink("/wiki/"+link.html()));
        }

        analyzed = new WikiPage(title, document.getWikiLink());
    }

    @Override
    public Collection<WikiLink> getLinks() {
        return linksFound;
    }

    @Override
    public WikiPage getPage() {
        return analyzed;
    }
}
