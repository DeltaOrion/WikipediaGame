package me.jacob.proj.crawl.analysis;

import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.model.WikiPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RedirectDocumentAnalyzer extends AbstractDocumentAnalyzer {

    private WikiPage analyzed;
    private final static String REDIRECT_ELE = "redirectMsg";

    private final static String LANG_ELE = "p-lang";

    @Override
    public void analyze() throws MalformedPageException {
        Document html = getDocument().getDocument();
        Element body = html.body();

        String title = getTitle(body);
        WikiPage page = new WikiPage(title,getDocument().getWikiLink());

        Elements content = body.getElementsByClass(REDIRECT_ELE);
        Element redirects = content.first();
        if(redirects!=null)
            harvestLinks(redirects);

        Element languages = body.getElementById(LANG_ELE);
        if(languages==null)
            throw new MalformedPageException();

        harvestLangs(page,languages);

        page.setRedirect(true);
        page.setDescription(title+" redirect");

        analyzed = page;
    }

    @Override
    public WikiPage getPage() {
        return analyzed;
    }
}
