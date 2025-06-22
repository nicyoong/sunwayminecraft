package com.sunwayMinecraft.regions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class RegionManager {
    private final JavaPlugin plugin;
    private Connection connection;
    private final Map<String, Region> regions = new HashMap<>();
    private final List<Region> regionSpatialIndex = new ArrayList<>();

    public RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
}
