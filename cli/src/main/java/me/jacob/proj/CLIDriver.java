package me.jacob.proj;

import me.jacob.proj.controller.CommandLineInterface;
import me.jacob.proj.controller.CrawlerCommand;
import me.jacob.proj.controller.CrawlerExitCommand;
import me.jacob.proj.controller.WikipediaCommand;

public class CLIDriver {

    public static void main(String[] args) {
        WikipediaGame game = new WikipediaGame();
        CommandLineInterface cli = new CommandLineInterface();
        cli.getCommandMap().registerCommand(new CrawlerCommand(game));
        cli.getCommandMap().registerCommand(new CrawlerExitCommand(cli,game));
        cli.getCommandMap().registerCommand(new WikipediaCommand(game));
        cli.run();
    }
}
