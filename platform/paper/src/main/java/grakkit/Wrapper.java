package grakkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wrapper extends Command {

    /** The current command executor. */
    public Value executor;

    /** The current tab completer. */
    public Value tabCompleter;

    /**
     * Constructs a new wrapper with the given parameters.W
     *
     * @param name    the command name
     * @param aliases the command aliases
     */
    public Wrapper(String name, String[] aliases) {
        super(name, "", "", new ArrayList<String>(Arrays.asList(aliases)));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (this.executor != null && this.executor.canExecute()) {
            try {
                this.executor.executeVoid(sender, label, args);
            } catch (Throwable error) {
                error.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        ArrayList<String> output = new ArrayList<>();
        if (this.tabCompleter != null && this.tabCompleter.canExecute()) {
            try {
                Value input = this.tabCompleter.execute(sender, alias, args);
                if (input != null && input.hasArrayElements()) {
                    for (long index = 0; index < input.getArraySize(); index++) {
                        Value value = input.getArrayElement(index);
                        if (value.isString()) {
                            output.add(value.asString());
                        }
                    }
                }
            } catch (Throwable error) {
                error.printStackTrace();
            }
        }
        return output;
    }

    /**
     * Sets options for this command.
     *
     * @param permission    the command permission
     * @param message       the command message
     * @param executor      the command executor
     * @param tabCompleter the command tab-completer
     */
    public void options(String permission, String message, Value executor, Value tabCompleter) {
        this.setPermission(permission);
        this.setPermissionMessage(message);
        this.executor = executor;
        this.tabCompleter = tabCompleter;
    }
}