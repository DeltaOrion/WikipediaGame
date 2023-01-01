package me.jacob.proj.controller;

import me.jacob.proj.Command;
import me.jacob.proj.WikipediaGame;
import picocli.CommandLine;

import java.util.Collections;

@CommandLine.Command(name = "exit", description = "safely terminates the program, shutting down the crawler and saving all data")
public class CrawlerExitCommand extends Command {

    private final CommandLineInterface cli;
    private final WikipediaGame wikipediaGame;

    public CrawlerExitCommand(CommandLineInterface cli, WikipediaGame wikipediaGame) {
        this.cli = cli;
        this.wikipediaGame = wikipediaGame;
    }

    @Override
    public Iterable<String> onTabCompletion(String[] current) {
        return Collections.emptyList();
    }

    @Override
    public void run() {
        wikipediaGame.shutdown();
        cli.setRunning(false);
        System.out.println("Goodbye :)");
    }
}
