package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite storage for active contracts, abandonment/acceptance cooldowns and
 * completion statistics. Mirrors the CoinFlipDatabase lifecycle: the
 * connection stays open for the plugin's life and must be closed on disable
 * (Windows keeps open files locked otherwise).
 */
public class ContractDatabase {
    private final JavaPlugin plugin;
    private Connection connection;

    public ContractDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + plugin.getDataFolder() + "/contracts.db");
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize contract database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_active_contracts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_uuid TEXT NOT NULL,"
                    + "contract_id TEXT NOT NULL,"
                    + "accepted_at INTEGER NOT NULL,"
                    + "expires_at INTEGER NOT NULL,"
                    + "progress REAL NOT NULL DEFAULT 0,"
                    + "progress_state TEXT,"
                    + "status TEXT NOT NULL DEFAULT 'active',"
                    + "UNIQUE(player_uuid, contract_id))");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_active_player"
                    + " ON player_active_contracts (player_uuid, status)");
            stmt.execute("CREATE TABLE IF NOT EXISTS contract_completion_stats ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_uuid TEXT NOT NULL,"
                    + "contract_id TEXT NOT NULL,"
                    + "alignment_id TEXT,"
                    + "campus_id TEXT,"
                    + "completed_at INTEGER NOT NULL,"
                    + "reward_amount REAL NOT NULL,"
                    + "reputation_awarded INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS contract_cooldowns ("
                    + "player_uuid TEXT NOT NULL,"
                    + "contract_id TEXT NOT NULL,"
                    + "cooldown_until INTEGER NOT NULL,"
                    + "PRIMARY KEY (player_uuid, contract_id))");
        }
    }

    /** Inserts or replaces the single active instance of a contract for a player. */
    public void addActiveContract(ActiveContract contract) {
        String sql = "INSERT OR REPLACE INTO player_active_contracts"
                + " (player_uuid, contract_id, accepted_at, expires_at, progress, progress_state,"
                + " status) VALUES (?, ?, ?, ?, ?, ?, 'active')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, contract.getPlayerUuid().toString());
            stmt.setString(2, contract.getContractId());
            stmt.setLong(3, contract.getStartTime().toEpochMilli());
            stmt.setLong(4, contract.getExpiryTime().toEpochMilli());
            stmt.setDouble(5, contract.getProgress());
            stmt.setString(6, contract.getProgressState());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding active contract: " + e.getMessage());
        }
    }

    public void removeActiveContract(UUID playerUuid, String contractId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM player_active_contracts WHERE player_uuid = ? AND contract_id = ?")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, contractId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing active contract: " + e.getMessage());
        }
    }

    /** All active rows; a player may hold several different active contracts. */
    public java.util.List<ActiveContract> getActiveContracts() {
        java.util.List<ActiveContract> contracts = new java.util.ArrayList<>();
        String sql = "SELECT player_uuid, contract_id, accepted_at, expires_at, progress,"
                + " progress_state FROM player_active_contracts WHERE status = 'active'";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                ActiveContract contract = new ActiveContract(player, rs.getString("contract_id"),
                        instant(rs.getLong("accepted_at")), instant(rs.getLong("expires_at")));
                contract.setProgress(rs.getDouble("progress"));
                contract.setProgressState(rs.getString("progress_state"));
                contracts.add(contract);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading active contracts: " + e.getMessage());
        }
        return contracts;
    }

    public boolean hasActiveContract(UUID playerUuid, String contractId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM player_active_contracts"
                        + " WHERE player_uuid = ? AND contract_id = ? AND status = 'active'")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, contractId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking active contract: " + e.getMessage());
            return false;
        }
    }

    public void updateProgressState(UUID playerUuid, String contractId, String progressState) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_active_contracts SET progress_state = ?"
                        + " WHERE player_uuid = ? AND contract_id = ?")) {
            stmt.setString(1, progressState);
            stmt.setString(2, playerUuid.toString());
            stmt.setString(3, contractId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating contract progress: " + e.getMessage());
        }
    }

    /** Marks overdue rows expired; returns how many changed. */
    public int expireContracts(long nowEpochMillis) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_active_contracts SET status = 'expired'"
                        + " WHERE status = 'active' AND expires_at < ?")) {
            stmt.setLong(1, nowEpochMillis);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error expiring contracts: " + e.getMessage());
            return 0;
        }
    }

    public void recordCompletion(UUID playerUuid, String contractId, String alignmentId,
                                 String campusId, Instant completedAt,
                                 double rewardAmount, long reputationAwarded) {
        String sql = "INSERT INTO contract_completion_stats"
                + " (player_uuid, contract_id, alignment_id, campus_id, completed_at,"
                + " reward_amount, reputation_awarded) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, contractId);
            stmt.setString(3, alignmentId);
            stmt.setString(4, campusId);
            stmt.setLong(5, completedAt.toEpochMilli());
            stmt.setDouble(6, rewardAmount);
            stmt.setLong(7, reputationAwarded);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error recording completion: " + e.getMessage());
        }
    }

    public Map<UUID, Map<String, Instant>> loadCooldowns() {
        Map<UUID, Map<String, Instant>> cooldowns = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, contract_id, cooldown_until FROM contract_cooldowns");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                        .put(rs.getString("contract_id"), instant(rs.getLong("cooldown_until")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading cooldowns: " + e.getMessage());
        }
        return cooldowns;
    }

    public void saveCooldowns(Map<UUID, Map<String, Instant>> cooldowns) {
        try {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement();
                 PreparedStatement stmt = connection.prepareStatement(
                         "INSERT OR REPLACE INTO contract_cooldowns"
                                 + " (player_uuid, contract_id, cooldown_until) VALUES (?, ?, ?)")) {
                clear.execute("DELETE FROM contract_cooldowns");
                for (Map.Entry<UUID, Map<String, Instant>> player : cooldowns.entrySet()) {
                    for (Map.Entry<String, Instant> entry : player.getValue().entrySet()) {
                        stmt.setString(1, player.getKey().toString());
                        stmt.setString(2, entry.getKey());
                        stmt.setLong(3, entry.getValue().toEpochMilli());
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving cooldowns: " + e.getMessage());
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ignored) {
                // rollback failure is unactionable; the severe log above covers it
            }
        } finally {
            try {
                if (connection != null) connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connection is closing or already reset
            }
        }
    }

    /** Row count helper for tests and diagnostics. */
    public int countActiveContracts() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM player_active_contracts WHERE status = 'active'")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private static Instant instant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.MILLIS);
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing contract database: " + e.getMessage());
            }
        }
    }
}
