package com.sunwayMinecraft.benches;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * The RegionManager class manages a set of cuboid regions for benches in the Minecraft world. It is
 * responsible for loading the regions from the plugin's configuration, checking if a specific
 * location is inside any of the defined regions, and providing access to region data.
 *
 * <p>The regions are stored in a map, with the region name as the key and the `CuboidRegion`
 * instance as the value. Each region is defined by two opposite corner locations (pos1 and pos2),
 * and the world where the region is located.
 *
 * <p>Key functionality includes: - Loading bench regions from the configuration file
 * (`benches.yml`), which contains information about the region's world and corner positions. -
 * Checking if a given location is within any of the defined cuboid regions. - Providing methods to
 * access regions by name, get the region names, or get the region at a specific location. -
 * Reloading regions from the configuration file.
 *
 * <p>The main methods provided by this class are: - `reloadRegions()`: Reloads the configuration
 * and loads the regions again. - `isInRegion(Location location)`: Checks if the provided location
 * is inside any of the cuboid regions. - `getRegionNames()`: Returns a list of all region names. -
 * `getRegion(String name)`: Retrieves a `CuboidRegion` by its name. - `getRegionAt(Location loc)`:
 * Retrieves the name of the region that contains the provided location.
 *
 * <p>The region data is loaded from a configuration file, and the regions are stored in a `Map`
 * where the key is the region name (from the configuration file) and the value is the
 * `CuboidRegion` object.
 */
public class BenchRegionManager {
  private final JavaPlugin plugin;
  private final BenchesConfigManager configManager;
  private final Map<String, CuboidRegion> regions = new HashMap<>();

  public BenchRegionManager(JavaPlugin plugin, BenchesConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    loadRegions();
  }

  private void loadRegions() {
    regions.clear();
    FileConfiguration config = configManager.getConfig();

    if (config.contains("benches")) {
      for (String regionKey : config.getConfigurationSection("benches").getKeys(false)) {
        String basePath = "benches." + regionKey + ".";
        try {
          String worldName = config.getString(basePath + "world");
          Location pos1 = parseLocation(config, basePath + "pos1", worldName);
          Location pos2 = parseLocation(config, basePath + "pos2", worldName);

          if (pos1 != null && pos2 != null) {
            // Store with region name as key
            regions.put(regionKey, new CuboidRegion(worldName, pos1, pos2));
          }
        } catch (Exception e) {
          plugin
              .getLogger()
              .warning("Failed to load bench region '" + regionKey + "': " + e.getMessage());
        }
      }
    }
  }

  private Location parseLocation(FileConfiguration config, String path, String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().warning("Invalid world '" + worldName + "' in bench configuration");
      return null;
    }

    return new Location(
        world,
        config.getDouble(path + ".x"),
        config.getDouble(path + ".y"),
        config.getDouble(path + ".z"));
  }

  public void reloadRegions() {
    configManager.reloadConfig();
    loadRegions();
  }

  public boolean isInRegion(Location location) {
    return regions.values().stream().anyMatch(region -> region.contains(location));
  }

  public List<String> getRegionNames() {
    return new ArrayList<>(regions.keySet());
  }

  public CuboidRegion getRegion(String name) {
    return regions.get(name);
  }

  public String getRegionAt(Location loc) {
    return regions.entrySet().stream()
        .filter(e -> e.getValue().contains(loc))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
