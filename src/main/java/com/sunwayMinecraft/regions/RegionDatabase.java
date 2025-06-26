package com.sunwayMinecraft.regions;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class RegionDatabase {
    private final JavaPlugin plugin;
    private Connection connection;

    public RegionDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, "regions.db");
        String url = "jdbc:sqlite:" + dbFile.getPath();
        connection = DriverManager.getConnection(url);
    }

    public void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
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

            stmt.execute("CREATE TABLE IF NOT EXISTS region_trust (" +
                    "region_id INTEGER NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "PRIMARY KEY (region_id, player_uuid)," +
                    "FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE)");
        }
    }

    public List<Region> loadAllRegions() throws SQLException {
        List<Region> regions = new ArrayList<>();
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

                Set<UUID> trusted = loadTrustedPlayers(id);
                regions.add(new Region(id, name, world, minX, minY, minZ,
                        maxX, maxY, maxZ, claimId, decoupled, trusted));
            }
        }
        return regions;
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

    public int createRegion(String name, String world, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, Long claimId, boolean decoupled) throws SQLException {
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

            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    public void updateRegionBounds(int id, int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ) throws SQLException {
        String sql = "UPDATE regions SET minX=?, minY=?, minZ=?, maxX=?, maxY=?, maxZ=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, minX);
            stmt.setInt(2, minY);
            stmt.setInt(3, minZ);
            stmt.setInt(4, maxX);
            stmt.setInt(5, maxY);
            stmt.setInt(6, maxZ);
            stmt.setInt(7, id);
            stmt.executeUpdate();
        }
    }

    public void deleteRegion(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM regions WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void setDecoupled(int id, boolean decoupled) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE regions SET decoupled=? WHERE id=?")) {
            stmt.setBoolean(1, decoupled);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void manageTrust(int regionId, UUID player, boolean add) throws SQLException {
        String sql = add ?
                "INSERT OR IGNORE INTO region_trust (region_id, player_uuid) VALUES (?, ?)" :
                "DELETE FROM region_trust WHERE region_id=? AND player_uuid=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, regionId);
            stmt.setString(2, player.toString());
            stmt.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }
}