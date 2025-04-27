package com.sunwayMinecraft.benches;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RegionManager {
    private final JavaPlugin plugin;
    private final BenchesConfigManager configManager;
    private final Map<String, CuboidRegion> regions = new HashMap<>();

    public RegionManager(JavaPlugin plugin, BenchesConfigManager configManager) {
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
                    plugin.getLogger().warning("Failed to load bench region '" + regionKey + "': " + e.getMessage());
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
                config.getDouble(path + ".z")
        );
    }

    public void reloadRegions() {
        configManager.reloadConfig();
        loadRegions();
    }

    public boolean isInRegion(Location location) {
        return regions.values().stream()
                .anyMatch(region -> region.contains(location));
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