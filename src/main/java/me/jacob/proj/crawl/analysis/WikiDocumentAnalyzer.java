package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.WebDocument;
import me.jacob.proj.crawl.fetch.DocumentFetcher;
import me.jacob.proj.crawl.fetch.WebDocumentFetcher;
import me.jacob.proj.model.WikiPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class WikiDocumentAnalyzer extends AbstractDocumentAnalyzer {

    private WikiPage analyzed;

    private final static String MAIN_CONTENT = "mw-content-text";
    private final static String LANG_ELE = "p-lang";

    private final static String REDIRECT_ELE = "redirectsub";

    private final static Pattern COMMA_PATTERN = Pattern.compile(",\\s*");

    public static void main(String[] args) throws MalformedURLException, MalformedPageException {
        DocumentFetcher fetcher = new WebDocumentFetcher();
        WebDocument doc = fetcher.fetch(new URL("https://en.wikipedia.org/wiki/UK_(disambiguation)"));
        DocumentAnalyzer analyzer = new WikiDocumentAnalyzer();
        analyzer.setDocument(doc);
        analyzer.analyze();

        System.out.println(analyzer.getLinks());
        System.out.println(analyzer.getPage().getArticleType());
    }


    @Override
    public WikiPage getPage() {
        return analyzed;
    }

    @Override
    public void analyze() throws MalformedPageException {
        Document html = getDocument().getDocument();
        if(html==null)
            throw new MalformedPageException("no page for link");

        if(isRedLink(html)) {
            throw new MalformedPageException("Red Link");
        }

        if(isMainPage(html))
            throw new MalformedPageException("Main Page");


        if(isRedirect(html)) {
            handleRedirect();
            return;
        }

        Element body = html.body();
        String title = getTitle(body);

        WikiPage page = new WikiPage(title, getDocument().getWikiLink());

        page.setArticleType(getDocumentType(getDocument().getWikiLink(),html));

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

    private boolean isMainPage(Document html) {
        Element head = html.head();
        Elements links = head.getElementsByTag("link");
        for(Element link : links) {
            if(link.attr("rel").equals("canonical")) {
                String href = link.attr("href");
                if(href.equals("https://en.wikipedia.org/wiki/Main_Page"))
                    return true;
            }
        }
        return false;
    }

    private void handleRedirect() throws MalformedPageException {
        DocumentAnalyzer redirectAnalyzer = new RedirectDocumentAnalyzer();
        redirectAnalyzer.setDocument(getDocument());
        redirectAnalyzer.analyze();
        analyzed = redirectAnalyzer.getPage();
        getLinkSet().addAll(redirectAnalyzer.getLinks());
        return;
    }

    private boolean isRedirect(Document html) {
        Element redirect = html.getElementById(REDIRECT_ELE);
        return redirect!=null;
    }

    private boolean isRedLink(Document html) {
        Element head = html.head();
        Elements meta = head.getElementsByTag("meta");
        for(Element element : meta) {
            if(element.attr("name").equals("robots")) {
                String content = element.attr("content");
                for(String tag : COMMA_PATTERN.split(content)) {
                    if (tag.equals("noindex") || tag.equals("nofollow"))
                        return true;
                }
            }
        }
        return false;
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
}
