package com.sunwayMinecraft.switches;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import java.io.File;
import java.util.*;

public class LightConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration lightConfig;
    private File configFile;

    public LightConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "lightregions.yml");
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("lightregions.yml", false);
        }
        lightConfig = YamlConfiguration.loadConfiguration(configFile);
    }
}
