package com.sunwayMinecraft.districts.persistence;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * SQLite persistence for district control: one state row per district plus
 * an append-only capture history. Uses its own connection to alignments.db.
 */
public class DistrictControlRepository {
    /** Persisted control state for one district. */
    public record ControlStateRecord(
            String districtId, String controllerAlignmentId, String controllerGrandAllianceId,
            String contestState, String leadingAlignmentId, int controlProgress, long lastUpdated) {}

    /** One historical control change. */
    public record ControlHistoryRecord(
            long id, String districtId, String previousAlignmentId, String newAlignmentId,
            String contestState, long changedAt, String reason) {}

    private final JavaPlugin plugin;
    private Connection connection;

    public DistrictControlRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            connection =
                    DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/alignments.db");
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            connection = null;
            plugin.getLogger()
                    .log(Level.SEVERE, "Failed to initialize district control database: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS district_control_state ("
                            + "district_id TEXT PRIMARY KEY,"
                            + "controller_alignment_id TEXT,"
                            + "controller_grand_alliance_id TEXT,"
                            + "contest_state TEXT NOT NULL DEFAULT 'stable',"
                            + "leading_alignment_id TEXT,"
                            + "control_progress INTEGER NOT NULL DEFAULT 0,"
                            + "last_updated INTEGER NOT NULL)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS district_control_history ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "district_id TEXT NOT NULL,"
                            + "previous_alignment_id TEXT,"
                            + "new_alignment_id TEXT,"
                            + "contest_state TEXT,"
                            + "changed_at INTEGER NOT NULL,"
                            + "reason TEXT)");
        }
    }

    public boolean isAvailable() {
        return connection != null;
    }

    public Map<String, ControlStateRecord> loadAllStates() {
        Map<String, ControlStateRecord> states = new HashMap<>();
        if (!isAvailable()) return states;
        String sql = "SELECT * FROM district_control_state";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ControlStateRecord record = new ControlStateRecord(
                        rs.getString("district_id"),
                        rs.getString("controller_alignment_id"),
                        rs.getString("controller_grand_alliance_id"),
                        rs.getString("contest_state"),
                        rs.getString("leading_alignment_id"),
                        rs.getInt("control_progress"),
                        rs.getLong("last_updated"));
                states.put(record.districtId(), record);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error loading control states: " + e.getMessage());
        }
        return states;
    }

    public Optional<ControlStateRecord> getState(String districtId) {
        if (!isAvailable()) return Optional.empty();
        String sql = "SELECT * FROM district_control_state WHERE district_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, districtId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ControlStateRecord(
                            districtId,
                            rs.getString("controller_alignment_id"),
                            rs.getString("controller_grand_alliance_id"),
                            rs.getString("contest_state"),
                            rs.getString("leading_alignment_id"),
                            rs.getInt("control_progress"),
                            rs.getLong("last_updated")));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error loading control state: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Inserts or replaces the control state row. Returns false on failure. */
    public boolean saveState(ControlStateRecord record) {
        if (!isAvailable()) return false;
        String sql =
                "INSERT INTO district_control_state "
                        + "(district_id, controller_alignment_id, controller_grand_alliance_id, "
                        + "contest_state, leading_alignment_id, control_progress, last_updated) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(district_id) DO UPDATE SET "
                        + "controller_alignment_id = excluded.controller_alignment_id, "
                        + "controller_grand_alliance_id = excluded.controller_grand_alliance_id, "
                        + "contest_state = excluded.contest_state, "
                        + "leading_alignment_id = excluded.leading_alignment_id, "
                        + "control_progress = excluded.control_progress, "
                        + "last_updated = excluded.last_updated";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, record.districtId());
            pstmt.setString(2, record.controllerAlignmentId());
            pstmt.setString(3, record.controllerGrandAllianceId());
            pstmt.setString(4, record.contestState());
            pstmt.setString(5, record.leadingAlignmentId());
            pstmt.setInt(6, record.controlProgress());
            pstmt.setLong(7, record.lastUpdated());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error saving control state: " + e.getMessage());
            return false;
        }
    }

    public boolean appendHistory(ControlHistoryRecord record) {
        if (!isAvailable()) return false;
        String sql =
                "INSERT INTO district_control_history "
                        + "(district_id, previous_alignment_id, new_alignment_id, contest_state, changed_at, reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, record.districtId());
            pstmt.setString(2, record.previousAlignmentId());
            pstmt.setString(3, record.newAlignmentId());
            pstmt.setString(4, record.contestState());
            pstmt.setLong(5, record.changedAt());
            pstmt.setString(6, record.reason());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error appending control history: " + e.getMessage());
            return false;
        }
    }

    /** Recent history for one district, newest first. */
    public List<ControlHistoryRecord> getHistory(String districtId, int limit) {
        List<ControlHistoryRecord> history = new ArrayList<>();
        if (!isAvailable()) return history;
        String sql =
                "SELECT * FROM district_control_history WHERE district_id = ? "
                        + "ORDER BY changed_at DESC, id DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, districtId);
            pstmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new ControlHistoryRecord(
                            rs.getLong("id"),
                            rs.getString("district_id"),
                            rs.getString("previous_alignment_id"),
                            rs.getString("new_alignment_id"),
                            rs.getString("contest_state"),
                            rs.getLong("changed_at"),
                            rs.getString("reason")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error loading control history: " + e.getMessage());
        }
        return history;
    }

    /** Removes the control state row (history is kept). */
    public boolean clearState(String districtId) {
        if (!isAvailable()) return false;
        String sql = "DELETE FROM district_control_state WHERE district_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, districtId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error clearing control state: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Districts] Error closing control database: " + e.getMessage());
        }
    }
}
