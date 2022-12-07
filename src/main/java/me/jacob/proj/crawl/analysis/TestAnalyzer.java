package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.model.Wikipedia;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestAnalyzer implements DocumentAnalyzer {

    private WebDocument document;
    private Set<WikiLink> linksFound = new HashSet<>();
    private WikiPage analyzed = null;

    @Override
    public void setDocument(WebDocument document) {
       this.document = document;
    }

    @Override
    public WebDocument getDocument() {
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
