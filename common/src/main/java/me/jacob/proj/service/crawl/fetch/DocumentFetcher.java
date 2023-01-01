package me.jacob.proj.service.crawl.fetch;

import me.jacob.proj.service.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;

public interface DocumentFetcher {

    FetchResult fetch(WikiLink url);
}
