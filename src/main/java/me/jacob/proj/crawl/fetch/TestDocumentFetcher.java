package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.model.WikiLink;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

public class TestDocumentFetcher implements DocumentFetcher {

    private final static Pattern FORWARD_SLASH_SPLIT = Pattern.compile("/");

    @Override
    public WebDocument fetch(URL url) {
        String[] split = FORWARD_SLASH_SPLIT.split(url.getPath());
        String name = split[2];
        File file = new File("testpages",name+".txt");
        if(!file.exists()) {
            return null;
        }

        System.out.println("Fetched "+url);

        try {
            return new WebDocument(new WikiLink(url),Jsoup.parse(file));
        } catch (IOException e) {
            return null;
        }
    }
}
