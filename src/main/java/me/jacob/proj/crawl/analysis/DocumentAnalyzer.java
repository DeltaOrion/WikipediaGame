package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;

import java.util.Collection;

public interface DocumentAnalyzer {

    void setDocument(FetchResult document);

    FetchResult getDocument();

    void analyze() throws MalformedPageException;

    //until these links have been analyzed
    //we can't add the children to the wiki-page yet
    Collection<WikiLink> getLinks();

    WikiPage getPage();

}
