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
}
