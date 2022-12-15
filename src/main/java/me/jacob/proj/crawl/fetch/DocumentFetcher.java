package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;

import java.net.URL;

public interface DocumentFetcher {

    FetchResult fetch(WikiLink url);
}
