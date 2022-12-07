package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;

import java.util.Collection;

public interface DocumentAnalyzer {

    void setDocument(WebDocument document);

    WebDocument getDocument();

    void analyze() throws MalformedPageException;

    //until these links have been analyzed
    //we can't add the children to the wiki-page yet
    Collection<WikiLink> getLinks();

    WikiPage getPage();

}
