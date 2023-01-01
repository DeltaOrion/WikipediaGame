package me.jacob.proj;

import picocli.CommandLine;

public abstract class Command implements Runnable {

    private final String name;
    private final String description;
    private final CommandLine picoCommand;

    protected Command() {
        //object hasn't been initialized yet
        CommandLine thisCommand = new CommandLine(this);
        this.name = thisCommand.getCommandName();
        this.description = thisCommand.getUsageMessage();
        this.picoCommand = thisCommand;
    }


    public abstract Iterable<String> onTabCompletion(String[] current);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CommandLine getPicoCommand() {
        return picoCommand;
    }
}
