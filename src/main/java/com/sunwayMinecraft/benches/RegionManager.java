package com.sunwayMinecraft.benches;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class RegionManager {
    private final JavaPlugin plugin;
    private final BenchesConfigManager configManager;
    private final List<CuboidRegion> regions = new ArrayList<>();

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
                        regions.add(new CuboidRegion(worldName, pos1, pos2));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load bench region '" + regionKey + "': " + e.getMessage());
                }
            }
        }
    }
}
