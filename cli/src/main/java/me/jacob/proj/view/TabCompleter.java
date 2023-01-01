package me.jacob.proj.view;

import me.jacob.proj.Command;
import me.jacob.proj.CommandMap;
import me.jacob.proj.controller.CommandLineInterface;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.Arrays;
import java.util.List;

public class TabCompleter implements Completer {

    private final CommandLineInterface controller;
    private final CommandMap map;

    public TabCompleter(CommandLineInterface controller, CommandMap map) {
        this.controller = controller;
        this.map = map;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String currLine = line.line();
        String[] args = controller.split(currLine);
        if(args.length==1) {
            for(String name : map.getCommandNames()) {
                if(name.startsWith(args[0]))
                    candidates.add(new Candidate(name));
            }
        } else {
            Command command = map.getCommand(args[0]);
            if(command==null)
                return;

            String[] formatted = controller.commandArgs(args);
            try {
                for (String option : command.onTabCompletion(formatted)) {
                    String currType = args[args.length - 1];
                    if (option.startsWith(currType))
                        candidates.add(new Candidate(option));
                }
            } catch (Throwable e) {
                System.out.println("An error occurred when tab completing the command '"+command.getName()+"' with args '"+ Arrays.toString(formatted) +"'");
                e.printStackTrace();
            }
        }
    }
}
