package me.jacob.proj.controller;

import me.jacob.proj.Command;
import me.jacob.proj.WikipediaGame;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.view.DisplayWikiView;
import picocli.CommandLine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

@CommandLine.Command(name = "crawler", description = "Options for starting, stopping and getting information from the crawler")
public class CrawlerCommand extends Command {

    private final WikipediaGame game;

    @CommandLine.Parameters(index = "0", description = "What to do, options = [start,stop,stats]")
    private String option = null;

    @CommandLine.Option(names = {"--start-link"}, defaultValue = "https://en.wikipedia.org/wiki/Black_hole", description = "the link the crawler should start with crawling through the wiki")
    private String startLink = null;

    @CommandLine.Option(names = {"-d","--display-wiki"}, description = "whether to display the pages after the crawler has stopped")
    private boolean display = false;

    public CrawlerCommand(WikipediaGame game) {
        this.game = game;
    }

    @Override
    public Iterable<String> onTabCompletion(String[] current) {
        return new ArrayList<>();
    }

    @Override
    public void run() {
        if(option==null) {
            System.out.println("No option specified");
            return;
        }

        if(option.equals("start")) {
            startCrawler();
        } else if(option.equals("stop")) {
            stopCrawler();
        } else {
            System.out.println("Unknown Option");
        }
    }

    private void stopCrawler() {
        game.getCrawler().shutdown();
        if(display) {
            DisplayWikiView view = new DisplayWikiView();
            view.displayPages(game.getPageService());
        }
    }

    private void startCrawler() {
        try {
            WikiLink link = new WikiLink(new URL(startLink));
            game.getCrawler().start(link);
        } catch (MalformedURLException e) {
            System.out.println("Invalid Link '"+startLink+"'");
        } catch (InterruptedException e) {
            System.out.println("An error occurred when starting with link '"+startLink+"'");
        }
    }
}
