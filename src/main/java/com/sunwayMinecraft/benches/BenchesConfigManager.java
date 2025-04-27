package com.sunwayMinecraft.benches;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class BenchesConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

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
}
