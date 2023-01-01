package me.jacob.proj.controller;

import me.jacob.proj.Command;
import me.jacob.proj.WikipediaGame;
import me.jacob.proj.model.WikiPage;
import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.view.DisplayWikiView;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@CommandLine.Command(name = "wiki", description = "Options for controlling and viewing indexed wikipedia pages")
public class WikipediaCommand extends Command {

    @CommandLine.Parameters(index = "0", arity = "0..*", description = "The pages to query")
    private List<String> pages = null;

    @CommandLine.Option(names = {"-p"}, description = "Toggle to find the shortest path between two pages")
    private boolean findPath;

    private final static Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

    private final DisplayWikiView view;
    private final WikipediaGame game;

    public WikipediaCommand(WikipediaGame game) {
        this.game = game;
        this.view = new DisplayWikiView();
    }

    @Override
    public Iterable<String> onTabCompletion(String[] current) {
        return Collections.emptyList();
    }

    @Override
    public void run() {
        if(pages !=null) {
            pageMode();
        } else {
            wikiMode

                    ();
        }
    }

    private void wikiMode() {
        System.out.println("---- oO Wikipedia Oo ----");
        view.displayPages(game.getPageService());
    }

    private void pageMode() {
        boolean display = true;
        Collection<WikiPage> pages = getPagesFromArgs();
        if(pages.size()==0)
            return;

        if(findPath) {
            findPaths(pages);
            display = false;
        }

        if(display)
            displayPages(pages);
    }

    private Collection<WikiPage> getPagesFromArgs() {
        List<WikiPage> wikiPages = new ArrayList<>();
        for(String title : pages) {
            WikiPage page = game.getPageService().getPage(getTitle(title));
            if (page==null) {
                System.out.println("Unknown Page '"+title+"'");
                return new ArrayList<>();
            } else {
                wikiPages.add(page);
            }
        }
        return wikiPages;
    }

    private void findPaths(Collection<WikiPage> pages) {
        if(this.pages.size()==1) {
            System.out.println("only one page specified");
            return;
        }

        List<WikiPage> wikiPages = new ArrayList<>(pages);
        WikiPage page1 = wikiPages.get(0);

        for(int i=1;i<wikiPages.size();i++) {
            WikiPage page2 = wikiPages.get(i);
            view.displayShortestPaths(page1,page2,game.getPageService().getShortestPaths(
                    page1, page2));

            page1 = page2;
        }
    }

    private void displayPages(Collection<WikiPage> pages) {
        for(WikiPage page : pages) {
            view.displayPage(page);
        }
    }

    private String getTitle(String arg) {
        String[] split = UNDERSCORE_PATTERN.split(arg);
        StringBuilder title = new StringBuilder();
        for(int i=0;i<split.length;i++) {
            title.append(split[i]);
            if(i<split.length-1)
                title.append(' ');
        }

        return title.toString();
    }
}
