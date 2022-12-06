package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.WebDocument;
import org.jsoup.nodes.Document;

import java.net.URL;

public interface DocumentFetcher {

    WebDocument fetch(URL url);
}
