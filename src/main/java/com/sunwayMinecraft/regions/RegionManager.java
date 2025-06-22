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

    private void loadRegions() throws SQLException {
        regions.clear();
        regionSpatialIndex.clear();

        // Load regions
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, world, minX, minY, minZ, maxX, maxY, maxZ, claimId, decoupled FROM regions")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String world = rs.getString("world");
                int minX = rs.getInt("minX");
                int minY = rs.getInt("minY");
                int minZ = rs.getInt("minZ");
                int maxX = rs.getInt("maxX");
                int maxY = rs.getInt("maxY");
                int maxZ = rs.getInt("maxZ");
                Long claimId = rs.getObject("claimId", Long.class);
                boolean decoupled = rs.getBoolean("decoupled");

                // Load trusted players
                Set<UUID> trusted = loadTrustedPlayers(id);

                Region region = new Region(id, name, world, minX, minY, minZ,
                        maxX, maxY, maxZ, claimId, decoupled, trusted);
                regions.put(name.toLowerCase(), region);
                regionSpatialIndex.add(region);
            }
        }
    }

    private Set<UUID> loadTrustedPlayers(int regionId) throws SQLException {
        Set<UUID> trusted = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid FROM region_trust WHERE region_id = ?")) {
            stmt.setInt(1, regionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                trusted.add(UUID.fromString(rs.getString("player_uuid")));
            }
        }
        return trusted;
    }
}
