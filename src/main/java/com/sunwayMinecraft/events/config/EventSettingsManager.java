package com.sunwayMinecraft.events.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EventSettingsManager {
    private final JavaPlugin plugin;
    private final File configFile;

    private boolean autoAnnouncements = true;
    private int maxActiveEvents = 1;

    public EventSettingsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "city-event-settings.yml");
        if (!configFile.exists()) {
            plugin.saveResource("city-event-settings.yml", false);
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        autoAnnouncements = config.getBoolean("auto_announcements", true);
        maxActiveEvents = config.getInt("max_active_events", 1);
    }

    public boolean isAutoAnnouncements() { return autoAnnouncements; }
    public int getMaxActiveEvents() { return maxActiveEvents; }
}
