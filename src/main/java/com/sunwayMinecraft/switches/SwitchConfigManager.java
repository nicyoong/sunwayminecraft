package com.sunwayMinecraft.switches;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import java.io.File;
import java.util.*;

public class SwitchConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration switchConfig;
    private File configFile;

    public SwitchConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "switches.yml");
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("switches.yml", false);
        }
        switchConfig = YamlConfiguration.loadConfiguration(configFile);
    }
}
