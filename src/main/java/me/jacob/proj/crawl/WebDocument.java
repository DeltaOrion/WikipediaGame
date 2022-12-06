package me.jacob.proj.crawl;

import me.jacob.proj.model.WikiLink;
import org.jsoup.nodes.Document;

public class WebDocument {

    private final WikiLink wikiLink;
    private final Document document;

    public WebDocument(WikiLink wikiLink, Document document) {
        this.wikiLink = wikiLink;
        this.document = document;
    }

    public WikiLink getWikiLink() {
        return wikiLink;
    }

    public Document getDocument() {
        return document;
    }
}
