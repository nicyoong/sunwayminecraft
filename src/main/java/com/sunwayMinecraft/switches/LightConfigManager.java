package com.sunwayMinecraft.switches;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import java.io.File;
import java.util.*;

/**
 * The LightConfigManager class handles loading, reloading, and parsing of the "lightregions.yml"
 * configuration file for defining light regions in the plugin. It encapsulates access to the YAML
 * file and converts its contents into {@link LightRegion} instances.
 *
 * <p>When initialized, the manager points to a "lightregions.yml" file in the plugin's data folder.
 * Calling {@link #reload()} will ensure the file exists (copying the default resource if necessary)
 * and load its contents into a {@link FileConfiguration} object.
 *
 * <p>Key functionality includes:
 *
 * <ul>
 *   <li>{@link #reload()}: Ensures the "lightregions.yml" file exists and loads or reloads its
 *       contents.
 *   <li>{@link #getRegions()}: Parses the "regions" section of the loaded configuration and returns
 *       a map of region names to {@link LightRegion} objects, each containing the world and
 *       bounding coordinates.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * LightConfigManager cfgManager = new LightConfigManager(myPlugin);
 * cfgManager.reload();
 * Map<String, LightRegion> regions = cfgManager.getRegions();
 * regions.forEach((name, region) -> {
 *     // process each region
 * });
 * }</pre>
 */
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

  /**
   * Retrieves all defined light regions from the loaded configuration.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Creates an empty {@link Map} to hold region names and their {@link LightRegion} objects.
   *   <li>Obtains the {@code "regions"} {@link org.bukkit.configuration.ConfigurationSection}; if
   *       it is {@code null}, returns an empty map.
   *   <li>Iterates over each region key, reads the world name and minimum/maximum coordinates from
   *       the configuration.
   *   <li>Creates a new {@link LightRegion} for each entry and puts it into the map.
   * </ol>
   *
   * <p>This method is typically used to fetch all configured regions for later processing (e.g.,
   * applying dynamic lighting rules).
   *
   * @return a {@link Map} mapping region names to {@link LightRegion} instances; returns an empty
   *     map if no regions are defined or if the configuration section is missing
   */
  public Map<String, LightRegion> getRegions() {
    Map<String, LightRegion> regions = new HashMap<>();
    ConfigurationSection regionsSection = lightConfig.getConfigurationSection("regions");

    if (regionsSection == null) return regions;

    for (String regionName : regionsSection.getKeys(false)) {
      String path = "regions." + regionName + ".";
      World world = plugin.getServer().getWorld(lightConfig.getString(path + "world"));

      regions.put(
          regionName,
          new LightRegion(
              regionName,
              world,
              lightConfig.getInt(path + "min.x"),
              lightConfig.getInt(path + "min.y"),
              lightConfig.getInt(path + "min.z"),
              lightConfig.getInt(path + "max.x"),
              lightConfig.getInt(path + "max.y"),
              lightConfig.getInt(path + "max.z")));
    }
    return regions;
  }
}
