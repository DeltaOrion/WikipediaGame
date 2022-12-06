package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.crawl.fetch.DocumentFetcher;
import me.jacob.proj.crawl.fetch.TestDocumentFetcher;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class WikiDocumentAnalyzer implements DocumentAnalyzer {

    private WebDocument document;
    private final Set<WikiLink> linksFound;
    private WikiPage analyzed;
    private boolean realPage;

    private final static String MAIN_CONTENT = "mw-content-text";
    private final static String TITLE = "mw-page-title-main";
    private final static String LANG_ELE = "p-lang";

    private final static Pattern DOT_PATTERN = Pattern.compile("\\.");
    private final static Pattern FILE_PATTERN = Pattern.compile("File:");

    public static void main(String[] args) throws MalformedURLException, MalformedPageException {
        DocumentFetcher fetcher = new TestDocumentFetcher();
        WebDocument doc = fetcher.fetch(new URL("https://en.wikipedia.org/wiki/Boundary_(topology)"));
        DocumentAnalyzer analyzer = new WikiDocumentAnalyzer();
        analyzer.setDocument(doc);
        analyzer.analyze();
    }

    public WikiDocumentAnalyzer() {
        document = null;
        linksFound = new HashSet<>();
    }

    @Override
    public void setDocument(WebDocument document) {
        this.document = document;
    }

    @Override
    public WebDocument getDocument() {
        return document;
    }

    @Override
    public Set<WikiLink> getLinks() {
        return Collections.unmodifiableSet(linksFound);
    }

    @Override
    public WikiPage getPage() {
        return analyzed;
    }

    @Override
    public void analyze() throws MalformedPageException {
        Document html = document.getDocument();
        if(html==null)
            throw new MalformedPageException("no page for link");

        Element body = html.body();
        String title = getTitle(body);

        WikiPage page = new WikiPage(title, document.getWikiLink());

        Element langs = body.getElementById(LANG_ELE);
        harvestLangs(page,langs);

        Element mainContent = body.getElementById(MAIN_CONTENT);
        if(mainContent==null)
            throw new MalformedPageException("article contains no content");

        page.setDescription(getDescription(mainContent));
        //harvest links from main content && cats
        harvestLinks(mainContent);

        Element catContent = body.getElementById("catlinks");
        if(catContent!=null)
            harvestLinks(catContent);

        analyzed = page;
    }

    private void harvestLangs(WikiPage page, Element langs) throws MalformedPageException {
        for(Element element : langs.getElementsByTag("li")) {
            Elements a = element.getElementsByTag("a");
            Element first = a.first();
            String href = first.attr("href");
            try {
                URL url = new URL(href);
                addLang(page,url);
            } catch (MalformedURLException e) {
                throw new MalformedPageException(e);
            }
        }
    }

    private void addLang(WikiPage page, URL url) {
        String path = url.getPath();
        String auth = url.getAuthority();
        Locale lang = new Locale(DOT_PATTERN.split(auth)[0]);
        page.getLink().addSupportedLang(lang,path);
    }

    private void harvestLinks(Element mainContent) {
        Elements links = mainContent.getElementsByTag("a");
        List<String> found = new ArrayList<>();
        for(Element link : links) {
            String url = link.attr("href");
            if(!url.equals(""))
                found.add(url);
        }

        sortURIS(found);
    }

    private void sortURIS(List<String> hrefs) {
        for(String href : hrefs) {
            try {
                URI uri = new URI(href);
                analyzeURI(uri);
            } catch (URISyntaxException ignored) {

            }
        }
    }

    private void analyzeURI(URI uri) {
        //we only care about internal links!
        if(uri.getAuthority()!=null)
            return;

        String path = uri.getRawPath();
        if(path.isEmpty())
            return;

        if(FILE_PATTERN.matcher(path).find())
            return;

        linksFound.add(new WikiLink(path));
    }

    private String getDescription(Element content) {
        //get first paragraph of main content;
        Elements paragraphs = content.getElementsByTag("p");
        for(Element element : paragraphs) {
            if(element.classNames().isEmpty() && element.id().equals("")) {
                //we found the first paragraph - extract the text.
                return convert(element);
            }

        }
        return "";
    }

    private String convert(Element element) {
        StringBuilder builder = new StringBuilder();
        convert(element,builder);
        return builder.toString();
    }

    private void convert(Element element, StringBuilder builder) {
        if(element.classNames().contains("reference") && element.tagName().equals("sup"))
            return;

        for (Node node : element.childNodes()) {
            if(node instanceof TextNode) {
                builder.append(node);
            } else if(node instanceof Element) {
                convert((Element) node,builder);
            }
        }
    }


    private String getTitle(Element body) throws MalformedPageException {
        Elements title = body.getElementsByClass(TITLE);
        if(title.size() != 1)
            throw new MalformedPageException("Multiple wiki titles");

        return title.html();
    }
}
