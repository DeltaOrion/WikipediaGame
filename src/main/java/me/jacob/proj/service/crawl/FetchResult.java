package me.jacob.proj.service.crawl;

import me.jacob.proj.service.crawl.fetch.FetchStatus;
import me.jacob.proj.model.WikiLink;
import org.jsoup.nodes.Document;

public class FetchResult {

    private final WikiLink wikiLink;
    private final Document document;
    private final FetchStatus status;

    public FetchResult(WikiLink wikiLink, Document document) {
        this.wikiLink = wikiLink;
        this.document = document;
        this.status = FetchStatus.SUCCESS;
    }

    public FetchResult(WikiLink link , FetchStatus status) {
        this.wikiLink = link;
        this.document = null;
        this.status = status;
    }

    public WikiLink getWikiLink() {
        return wikiLink;
    }

    public Document getDocument() {
        return document;
    }

    public FetchStatus getStatus() {
        return status;
    }
}
