package com.sunwayMinecraft.regions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
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

    public void importLegacyRegions() {
        File legacyFile = new File(plugin.getDataFolder(), "lightregions.yml");
        if (!legacyFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        if (regionsSection == null) return;

        int imported = 0;
        for (String name : regionsSection.getKeys(false)) {
            String path = "regions." + name + ".";
            String worldName = config.getString(path + "world");
            int minX = config.getInt(path + "min.x");
            int minY = config.getInt(path + "min.y");
            int minZ = config.getInt(path + "min.z");
            int maxX = config.getInt(path + "max.x");
            int maxY = config.getInt(path + "max.y");
            int maxZ = config.getInt(path + "max.z");

            if (createRegion(name, worldName, minX, minY, minZ, maxX, maxY, maxZ, null, false)) {
                imported++;
            }
        }
        plugin.getLogger().info("Imported " + imported + " regions from legacy configuration");
    }

    public boolean createRegion(String name, String world, int minX, int minY, int minZ,
                                int maxX, int maxY, int maxZ, Long claimId, boolean decoupled) {
        // Validate coordinates
        if (minX > maxX || minY > maxY || minZ > maxZ) return false;

        // Check if region exists
        if (regions.containsKey(name.toLowerCase())) return false;

        // Insert into database
        String sql = "INSERT INTO regions (name, world, minX, minY, minZ, maxX, maxY, maxZ, claimId, decoupled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, world);
            stmt.setInt(3, minX);
            stmt.setInt(4, minY);
            stmt.setInt(5, minZ);
            stmt.setInt(6, maxX);
            stmt.setInt(7, maxY);
            stmt.setInt(8, maxZ);
            if (claimId != null) {
                stmt.setLong(9, claimId);
            } else {
                stmt.setNull(9, Types.BIGINT);
            }
            stmt.setBoolean(10, decoupled);
            stmt.executeUpdate();

            // Get generated ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                Region region = new Region(id, name, world, minX, minY, minZ,
                        maxX, maxY, maxZ, claimId, decoupled, new HashSet<>());
                regions.put(name.toLowerCase(), region);
                regionSpatialIndex.add(region);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create region: " + name, e);
        }
        return false;
    }

    public boolean updateRegionBounds(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Region region = regions.get(name.toLowerCase());
        if (region == null || region.isDecoupled()) return false;

        String sql = "UPDATE regions SET minX=?, minY=?, minZ=?, maxX=?, maxY=?, maxZ=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, minX);
            stmt.setInt(2, minY);
            stmt.setInt(3, minZ);
            stmt.setInt(4, maxX);
            stmt.setInt(5, maxY);
            stmt.setInt(6, maxZ);
            stmt.setInt(7, region.getId());
            stmt.executeUpdate();

            region.updateBounds(minX, minY, minZ, maxX, maxY, maxZ);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update region: " + name, e);
        }
        return false;
    }

    public boolean deleteRegion(String name) {
        Region region = regions.get(name.toLowerCase());
        if (region == null) return false;

        String sql = "DELETE FROM regions WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, region.getId());
            stmt.executeUpdate();

            regions.remove(name.toLowerCase());
            regionSpatialIndex.remove(region);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete region: " + name, e);
        }
        return false;
    }
}
