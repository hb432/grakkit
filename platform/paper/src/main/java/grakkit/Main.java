package grakkit;

import grakkit.api.Loader;
import grakkit.interop.NodeInterop; // Import NodeInterop
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.HashMap;

public final class Main extends JavaPlugin {
    // A list of all registered commands
    public static final HashMap<String, Wrapper> commands = new HashMap<>();

    // The internal command map used to register commands
    public static CommandMap registery;

    private String getPluginName() {
        // get the input stream for the plugin.yml file
        InputStream inputStream = getResource("plugin.yml");

        // Create an InputStreamReader using the input stream & UTF-8 encoding
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        // Load the YAML configuration from the reader
        YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);

        try {
            reader.close();
        } catch (Throwable e) {
            // none
        }
        return config.getString("name");
    }

    private void fixSQL () {
        // Black magic. This fixes a bug, as something is breaking SQL Integration for
        // other plugins.
        DriverManager.getDrivers();
        Grakkit.patch(new Loader(this.getClassLoader())); // CORE - patch class loader with GraalJS
        try {
            Field internal = this.getServer().getClass().getDeclaredField("commandMap");
            internal.setAccessible(true);
            Main.registery = (CommandMap) internal.get(this.getServer());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onLoad() {
        fixSQL();
    }
    @Override
    public void onEnable() {
        Grakkit.init(this.getDataFolder().getPath()); // CORE - initialize
        try {
            this.getServer().getScheduler().runTaskTimer(this, Grakkit::tick, 0, 1); // CORE - run task loop
        } catch (Throwable e) {
            // none
        }
    }
    private Value asRunnable(Wrapper command) {
        return Value.asValue((Runnable) () -> {});
    }
    @Override
    public void onDisable() {
        System.out.println("[Grakkit] Disabling Grakkit...");
        Grakkit.close(); // CORE - close before exist
        Main.commands.values().forEach(command -> {
            command.executor = asRunnable(command);
            command.tabCompleter = asRunnable(command);
        });
        System.out.println("[Grakkit] Grakkit disabled.");
    }
    // Registers a custom command to the server with the given options
    public void register(String namespace, String name, String[] aliases, String permission, String message, Value executor, Value tabCompleter) {
        String key = namespace + "." + name;
        Wrapper command;
        if (Main.commands.containsKey(key)) command = Main.commands.get(key);
        else {
            command = new Wrapper(name, aliases);
            Main.registery.register(namespace, command);
            Main.commands.put(key, command);
        }
        command.options(permission, message, executor, tabCompleter);
    }
}