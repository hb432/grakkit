package grakkit;

import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.nio.file.Paths;

public class GrakkitConfig {
    // The "main" property within the config
    public String main = "index.js";

    // The name/title to use on all platforms
    public String interopName = "Grakkit";

    // Should the JS initialization function be called?
    public boolean shouldInitialize = true;

    // Enables verbose logging
    public boolean verbose = false;

    // Enable full Node.js integration
    public boolean useNode = true; // Default to true

    // The configuration file used for setting these options
    public File configFile;

    // The config options
    public ConfigurationSection config;

    private File createConfig(File configFile) {
        try {
            YamlFile config = new YamlFile(this.configFile);
            config.createNewFile(true);
            config.set("main", "index.js");
            config.set("useNode", true); // Default to true in config
            config.save();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return configFile;
    }

    // Creates a Yaml configuration from the given values
    public GrakkitConfig(String root, String name) {
        this.configFile = Paths.get(root, "config.yml").toFile();
        if (!this.configFile.exists()) createConfig(this.configFile);
        try {
            YamlFile config = YamlFile.loadConfiguration(this.configFile);
            this.config = config.getRoot();
            this.main = this.config.getString("main", this.main);
            this.shouldInitialize = this.config.getBoolean("init", this.shouldInitialize);
            this.verbose = this.config.getBoolean("verbose", this.verbose);
            this.useNode = this.config.getBoolean("useNode", this.useNode);
        } catch (Throwable error) {
            throw new RuntimeException("An error occurred while reading the config file!", error);
        }
    }
}