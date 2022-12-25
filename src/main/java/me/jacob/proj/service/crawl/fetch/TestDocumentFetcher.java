package me.jacob.proj.service.crawl.fetch;

import me.jacob.proj.service.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.util.TestPage;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class TestDocumentFetcher implements DocumentFetcher {

    private final ConcurrentMap<String, TestPage> pages;
    private final static Pattern FORWARD_SLASH_SPLIT = Pattern.compile("/");

    public TestDocumentFetcher() {
        this.pages = new ConcurrentHashMap<>();
    }

    public TestPage getPage(URL link) {
        String[] split = FORWARD_SLASH_SPLIT.split(link.getPath());
        if(split.length<3)
            return null;
        String name = split[2];
        return pages.get(name);
    }

    public TestPage getPage(String link) {
        return pages.get(link);
    }

    public void addPage(TestPage page) {
        this.pages.putIfAbsent(page.getLink(),page);
    }

    public void removePage(TestPage page) {
        this.pages.remove(page.getLink());
    }

    @Override
    public FetchResult fetch(WikiLink url) {
        TestPage page = getPage(url.getLink());
        if(page==null)
            return new FetchResult(url,FetchStatus.DOES_NOT_EXIST);

        return new FetchResult(url,page.toDocument());
    }
}
