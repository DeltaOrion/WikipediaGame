package me.jacob.proj.crawl.fetch;

import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.model.WikiLink;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class WebDocumentFetcher implements DocumentFetcher {

    public static void main(String[] args) throws MalformedURLException {
        WebDocumentFetcher fetcher = new WebDocumentFetcher();
        System.out.println(fetcher.fetch(new WikiLink(new URL("https://en.wikipedia.org/admin"))).getStatus());
    }

    @Override
    public FetchResult fetch(WikiLink link) {
        try {
            URL url = link.getLink();
            Connection connection = Jsoup.connect(url.toString());
            connection.followRedirects(false);
            connection.timeout(10000);

            return new FetchResult(new WikiLink(url),connection.get());
        } catch (UnknownHostException | SocketTimeoutException e) {
            return new FetchResult(link,FetchStatus.CONNECTION_ERROR);
        } catch (IOException | ClassCastException e) {
            return new FetchResult(link,FetchStatus.DOES_NOT_EXIST);
        }
    }
}
