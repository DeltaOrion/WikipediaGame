package me.jacob.proj.controller;

import me.jacob.proj.Command;
import me.jacob.proj.CommandMap;
import me.jacob.proj.view.TabCompleter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.regex.Pattern;

public class CommandLineInterface {

    private final CommandMap commandMap;
    private boolean running;

    private final static Pattern WHITE_SPACE = Pattern.compile("\\s+");

    public CommandLineInterface() {
        this.commandMap = new CommandMap();
        this.running = true;
    }


    public void run() throws UserInterruptException {
        LineReader reader = LineReaderBuilder.builder()
                .completer(new TabCompleter(this,commandMap))
                .build();

        while (running) {
            String line = reader.readLine("> ");
            execute(line);
        }
    }

    private void execute(String line) {
        String[] args = split(line);
        if(args.length==0) {
            System.out.println("No Command Entered");
            return;
        }

        Command command = commandMap.getCommand(args[0]);
        if(command==null) {
            System.out.println("Unknown Command '"+args[0]+"'");
            return;
        }

        String[] formatted = commandArgs(args);
        try {
            CommandLine picoCommand = command.getPicoCommand();
            picoCommand.execute(formatted);
        } catch (Throwable e) {
            System.out.println("An error occurred when running the command '"+command+"' with args '"+ Arrays.toString(formatted) +"'");
            e.printStackTrace();
        }
    }

    public String[] commandArgs(String[] args) {
        return Arrays.copyOfRange(args,1,args.length);
    }

    public String[] split(String line) {
        return WHITE_SPACE.split(line);
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }

    public boolean isRunning() {
        return running;
    }

    //might want to interrupt this thread
    public void setRunning(boolean running) {
        this.running = running;
    }
}
