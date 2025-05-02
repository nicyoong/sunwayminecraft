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
