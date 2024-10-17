package com.sunwayMinecraft.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigLoader {

    /**
     * Returns the loaded configuration from the plugin.
     *
     * @param plugin The plugin instance
     * @return The configuration object
     */
    public static FileConfiguration getConfig(Plugin plugin) {
        plugin.saveDefaultConfig();
        return plugin.getConfig();
    }
}
