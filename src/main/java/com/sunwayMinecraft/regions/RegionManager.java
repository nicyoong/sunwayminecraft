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

    public void initializeDatabase() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, "regions.db");
        String url = "jdbc:sqlite:" + dbFile.getPath();

        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            // Create regions table
            stmt.execute("CREATE TABLE IF NOT EXISTS regions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL," +
                    "world TEXT NOT NULL," +
                    "minX INTEGER NOT NULL," +
                    "minY INTEGER NOT NULL," +
                    "minZ INTEGER NOT NULL," +
                    "maxX INTEGER NOT NULL," +
                    "maxY INTEGER NOT NULL," +
                    "maxZ INTEGER NOT NULL," +
                    "claimId INTEGER," +
                    "decoupled BOOLEAN NOT NULL DEFAULT 0)");

            // Create trust table
            stmt.execute("CREATE TABLE IF NOT EXISTS region_trust (" +
                    "region_id INTEGER NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "PRIMARY KEY (region_id, player_uuid)," +
                    "FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE)");

            // Load regions into memory
            loadRegions();
        }
    }
}
