package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.model.WikiLink;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;

public class WebDocumentFetcher implements DocumentFetcher {

    @Override
    public WebDocument fetch(URL url) {
        Connection connection = Jsoup.connect(url.toString());
        connection.timeout(10000);
        connection.request().followRedirects(false);
        try {
            Document doc = connection.get();
            return new WebDocument(new WikiLink(url),doc);
        } catch (IOException e) {
            return null;
        }
    }
}
