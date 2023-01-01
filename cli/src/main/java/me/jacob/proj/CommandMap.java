package me.jacob.proj;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CommandMap {

    private final ConcurrentMap<String, Command> commandMap;

    public CommandMap() {
        commandMap = new ConcurrentHashMap<>();
    }

    public void registerCommand(Command command) {
        this.commandMap.put(command.getName().toLowerCase(),command);
    }

    public Collection<Command> getCommands() {
        return commandMap.values();
    }

    public Collection<String> getCommandNames() {
        return commandMap.keySet();
    }

    public Command getCommand(String name) {
        if(name==null)
            return null;

        return this.commandMap.get(name.toLowerCase());
    }

    public boolean hasCommand(String name) {
        return this.commandMap.containsKey(name);
    }

    public void removeCommand(String name) {
        this.commandMap.remove(name.toLowerCase());
    }
}
