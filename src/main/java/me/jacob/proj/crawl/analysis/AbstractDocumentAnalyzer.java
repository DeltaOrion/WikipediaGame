package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractDocumentAnalyzer implements DocumentAnalyzer {

    private WebDocument document;
    private final Set<WikiLink> linksFound;
    private final static Pattern DOT_PATTERN = Pattern.compile("\\.");
    private final static Pattern FILE_PATTERN = Pattern.compile("File:");
    private final static Pattern COLON_PATTERN = Pattern.compile(":");

    private final static String TITLE = "mw-page-title-main";
    private final static String TITLE2 = "firstHeading";
    private final static String DISAMBIG = "dmbox-body";

    protected AbstractDocumentAnalyzer() {
        this.linksFound = new HashSet<>();
    }

    @Override
    public void setDocument(WebDocument document) {
        this.document = document;
        this.linksFound.clear();
    }

    @Override
    public WebDocument getDocument() {
        return document;
    }

    @Override
    public Collection<WikiLink> getLinks() {
        return Collections.unmodifiableSet(linksFound);
    }

    protected Set<WikiLink> getLinkSet() {
        return linksFound;
    }

    protected String getDocumentType(WikiLink link, Document document) {
        if (document.getElementsByClass(DISAMBIG).size() > 0)
            return "Disambiguation";

        String[] split = COLON_PATTERN.split(link.getRelative());
        if(split.length==2)
            return split[0];

        return null;
    }

    protected void harvestLangs(WikiPage page, Element langs) throws MalformedPageException {
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

    protected void harvestLinks(Element mainContent) {
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
        if(path == null || path.isEmpty())
            return;

        if(FILE_PATTERN.matcher(path).find())
            return;

        linksFound.add(new WikiLink(path));
    }

    protected String getTitle(Element body) throws MalformedPageException {
        Elements titleChoices = body.getElementsByClass(TITLE);
        Element title = null;
        if(titleChoices.size() >= 1)
            title = titleChoices.first();

        if(titleChoices.size()==0) {
            title = body.getElementById(TITLE2);
        }

        if(title == null)
            throw new MalformedPageException("No title");

        return title.text();
    }
}
