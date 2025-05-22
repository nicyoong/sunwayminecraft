package com.sunwayMinecraft.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigLoader {

  private static FileConfiguration beaconConfig;

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

  public static FileConfiguration loadBeaconLocations(Plugin plugin) {
    File beaconLocationsFile = new File(plugin.getDataFolder(), "beaconlocations.yml");

    // Check if the file exists; if not, create it with defaults
    if (!beaconLocationsFile.exists()) {
      plugin.saveResource("beaconlocations.yml", false); // Copies from the plugin jar
    }

    // Load the YAML file
    return YamlConfiguration.loadConfiguration(beaconLocationsFile);
  }

  public static FileConfiguration getBeaconConfig() {
    return beaconConfig;
  }
}
