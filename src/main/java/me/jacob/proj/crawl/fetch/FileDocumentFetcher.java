package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class FileDocumentFetcher implements DocumentFetcher {

    private final static Pattern FORWARD_SLASH_SPLIT = Pattern.compile("/");
    private final Path crawlDirectory;

    public FileDocumentFetcher(Path crawlDirectory) {
        this.crawlDirectory = crawlDirectory;
    }

    @Override
    public FetchResult fetch(WikiLink link) {
        URL url = link.getLink();
        String[] split = FORWARD_SLASH_SPLIT.split(url.getPath());
        String name = split[2];
        File file = crawlDirectory.resolve(name+".txt").toFile();
        if(!file.exists()) {
            return new FetchResult(link,FetchStatus.DOES_NOT_EXIST);
        }

        try {
            return new FetchResult(link,Jsoup.parse(file));
        } catch (IOException e) {
            return new FetchResult(link,FetchStatus.CONNECTION_ERROR);
        }
    }
}
