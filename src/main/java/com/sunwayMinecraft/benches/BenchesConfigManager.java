package com.sunwayMinecraft.benches;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/**
 * This class manages the configuration of benches for a Minecraft plugin. It is responsible for
 * loading, reloading, and providing access to the configuration file that stores bench-related
 * settings, ensuring that the configuration file is available and properly initialized.
 *
 * The BenchesConfigManager handles the process of saving the default configuration file
 * (`benches.yml`) to the plugin's data folder if it does not already exist. It also provides
 * methods for reloading the configuration and retrieving the current configuration settings.
 *
 * The main methods provided by this class are:
 * - `initializeConfig()`: Initializes the configuration by loading the `benches.yml` file.
 * - `reloadConfig()`: Reloads the configuration by calling the initialization method again.
 * - `getConfig()`: Retrieves the current FileConfiguration instance for accessing configuration
 *    data.
 *
 * The configuration file is loaded as a `FileConfiguration` object, which can be used to read
 * values like bench settings, layout, or other data stored in the `benches.yml` file.
 */
public class BenchesConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    /**
     * Constructs a new BenchesConfigManager instance with the provided JavaPlugin.
     * This constructor initializes the plugin reference and defers the configuration
     * loading process to a separate method for better separation of concerns.
     *
     * The constructor takes a JavaPlugin instance as a parameter, which is used
     * to interact with the plugin's data folder and manage the configuration file.
     * After storing the plugin reference, it calls the `initializeConfig()` method
     * to load the configuration file (`benches.yml`), ensuring that the necessary
     * configuration is available for use.
     *
     * @param plugin The JavaPlugin instance that this configuration manager will use
     *               to interact with the plugin's data folder and load resources.
     */
    public BenchesConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Defer reload to separate method
        initializeConfig();
    }

    private void initializeConfig() {
        plugin.saveResource("benches.yml", false);
        File configFile = new File(plugin.getDataFolder(), "benches.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        initializeConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }
}