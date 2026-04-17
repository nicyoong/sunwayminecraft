package com.sunwayMinecraft.residency.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public abstract class AbstractYamlConfigManager {
    protected final JavaPlugin plugin;
    protected final String fileName;
    protected File file;
    protected FileConfiguration config;

    protected AbstractYamlConfigManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    protected abstract void load();

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + fileName + ": " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() { return config; }
}
