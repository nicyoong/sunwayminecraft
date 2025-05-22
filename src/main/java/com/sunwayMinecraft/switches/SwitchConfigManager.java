package com.sunwayMinecraft.switches;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import java.io.File;
import java.util.*;

/**
 * The SwitchConfigManager class handles loading and parsing of the {@code switches.yml}
 * configuration file, and provides access to configured button switches mapped to their light
 * locations.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Locating or creating the {@code switches.yml} resource in the plugin’s data folder.
 *   <li>Loading the YAML configuration into a {@link
 *       org.bukkit.configuration.file.FileConfiguration}.
 *   <li>Parsing the {@code buttons} section into a map of button locations to {@link ButtonSwitch}
 *       objects.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * SwitchConfigManager configManager = new SwitchConfigManager(plugin);
 * configManager.reload();
 * Map<Location, ButtonSwitch> switches = configManager.getSwitches();
 * }</pre>
 */
public class SwitchConfigManager {
  private final JavaPlugin plugin;
  private FileConfiguration switchConfig;
  private File configFile;

  /**
   * Constructs a new SwitchConfigManager for the given plugin. The configuration file is assumed to
   * be named {@code switches.yml} in the plugin’s data folder.
   *
   * @param plugin the {@link JavaPlugin} instance used to locate the data folder and to save
   *     default resources
   */
  public SwitchConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "switches.yml");
  }

  /**
   * Reloads the {@code switches.yml} configuration.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>If the file does not exist in the plugin’s data folder, saves the default resource from
   *       the plugin JAR without overwriting any existing file.
   *   <li>Loads the YAML configuration into the {@link FileConfiguration} {@code switchConfig}
   *       field via {@link
   *       org.bukkit.configuration.file.YamlConfiguration#loadConfiguration(File)}.
   * </ol>
   */
  public void reload() {
    if (!configFile.exists()) {
      plugin.saveResource("switches.yml", false);
    }
    switchConfig = YamlConfiguration.loadConfiguration(configFile);
  }

  /**
   * Retrieves all defined button switches from the loaded configuration.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Creates an empty {@link Map} to hold button {@link Location} keys and their corresponding
   *       {@link ButtonSwitch} objects.
   *   <li>Obtains the {@code "buttons"} {@link org.bukkit.configuration.ConfigurationSection}; if
   *       it is {@code null}, logs a warning and returns an empty map.
   *   <li>Iterates over each button key string, parsing it into a {@link Location} via {@link
   *       #parseLocation(String)}; logs and skips invalid entries.
   *   <li>Retrieves the {@code lights} list for each button, parses each location string similarly,
   *       and constructs a {@link ButtonSwitch} with the results.
   *   <li>Puts the successfully parsed {@link ButtonSwitch} into the result map.
   * </ol>
   *
   * @return a {@link Map} mapping button {@link Location}s to {@link ButtonSwitch} instances;
   *     returns an empty map if no buttons are defined or if parsing fails
   */
  public Map<Location, ButtonSwitch> getSwitches() {
    Map<Location, ButtonSwitch> switches = new HashMap<>();
    ConfigurationSection buttonsSection = switchConfig.getConfigurationSection("buttons");

    if (buttonsSection == null) {
      plugin.getLogger().warning("No 'buttons' section found in switches.yml");
      return switches;
    }

    for (String key : buttonsSection.getKeys(false)) {
      Location buttonLoc = parseLocation(key);
      if (buttonLoc == null) {
        plugin.getLogger().warning("Failed to parse button location: " + key);
        continue;
      }

      ConfigurationSection buttonSection = buttonsSection.getConfigurationSection(key);
      if (buttonSection == null) {
        plugin.getLogger().warning("No configuration section for button: " + key);
        continue;
      }

      List<String> lightStrings = buttonSection.getStringList("lights");

      List<Location> lights = new ArrayList<>();
      for (String locStr : lightStrings) {
        Location loc = parseLocation(locStr);
        if (loc == null) {
          plugin.getLogger().warning("Invalid light location: " + locStr);
          continue;
        }
        lights.add(loc);
      }

      switches.put(buttonLoc, new ButtonSwitch(buttonLoc, lights));
    }
    return switches;
  }

  /**
   * Parses a location string in the format {@code "world,x,y,z"} into a {@link Location}.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Validates that the input string is non-null and non-empty; logs a warning if invalid.
   *   <li>Splits the string by commas into four parts: world name and x, y, z coordinates.
   *   <li>Resolves the world name to a {@link org.bukkit.World}; logs a warning if not found.
   *   <li>Attempts to parse the coordinate parts into integers; logs a warning on parse failure.
   *   <li>Constructs and returns a new {@link Location} if all parts are valid, or {@code null}
   *       otherwise.
   * </ol>
   *
   * @param str the location string in {@code "world,x,y,z"} format
   * @return the parsed {@link Location}, or {@code null} if parsing fails
   */
  private Location parseLocation(String str) {
    if (str == null || str.trim().isEmpty()) {
      plugin.getLogger().warning("Empty location string");
      return null;
    }

    // Split and trim parts
    String[] parts = str.trim().split("\\s*,\\s*");
    if (parts.length != 4) {
      plugin.getLogger().warning("Invalid location format (expected world,x,y,z): " + str);
      return null;
    }

    // Validate world
    String worldName = parts[0];
    World world = plugin.getServer().getWorld(worldName);
    if (world == null) {
      plugin.getLogger().warning("World '" + worldName + "' does not exist");
      return null;
    }

    // Parse coordinates
    try {
      int x = Integer.parseInt(parts[1]);
      int y = Integer.parseInt(parts[2]);
      int z = Integer.parseInt(parts[3]);
      return new Location(world, x, y, z);
    } catch (NumberFormatException e) {
      plugin.getLogger().warning("Invalid coordinates in location: " + str);
      return null;
    }
  }
}
