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
            stmt.execute("CREATE TABLE IF NOT EXISTS contract_influence_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "contract_id TEXT NOT NULL,"
                    + "player_uuid TEXT,"
                    + "origin_alignment_id TEXT,"
                    + "destination_alignment_id TEXT,"
                    + "origin_grand_alliance_id TEXT,"
                    + "destination_grand_alliance_id TEXT,"
                    + "influence_amount INTEGER NOT NULL,"
                    + "influence_type TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS alignment_influence_totals ("
                    + "alignment_id TEXT PRIMARY KEY,"
                    + "influence_total INTEGER NOT NULL DEFAULT 0,"
                    + "updated_at INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS alignment_supply_totals ("
                    + "alignment_id TEXT PRIMARY KEY,"
                    + "supply_total INTEGER NOT NULL DEFAULT 0,"
                    + "updated_at INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS contract_dynamic_contracts ("
                    + "contract_id TEXT PRIMARY KEY,"
                    + "template_id TEXT NOT NULL,"
                    + "expires_at INTEGER NOT NULL)");
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

    /** Loads one active row by its database id (sabotage targets by this id). */
    public ActiveContract getActiveContractById(int activeId) {
        String sql = "SELECT id, player_uuid, contract_id, accepted_at, expires_at, progress,"
                + " progress_state FROM player_active_contracts WHERE id = ? AND status = 'active'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, activeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                ActiveContract contract = new ActiveContract(
                        UUID.fromString(rs.getString("player_uuid")), rs.getString("contract_id"),
                        instant(rs.getLong("accepted_at")), instant(rs.getLong("expires_at")));
                contract.setActiveId(rs.getInt("id"));
                contract.setProgress(rs.getDouble("progress"));
                contract.setProgressState(rs.getString("progress_state"));
                return contract;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading active contract by id: " + e.getMessage());
            return null;
        }
    }

    /** Sabotage "delay" effect: pushes the expiry out and returns the progress before. */
    public void delayActiveContract(int activeId, long extraExpirySeconds) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_active_contracts SET expires_at = expires_at + ?,"
                        + " progress = ? WHERE id = ? AND status = 'active'")) {
            stmt.setLong(1, extraExpirySeconds * 1000L);
            stmt.setDouble(2, 0.0);
            stmt.setInt(3, activeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error delaying active contract: " + e.getMessage());
        }
    }

    /** All active rows; a player may hold several different active contracts. */
    public java.util.List<ActiveContract> getActiveContracts() {
        java.util.List<ActiveContract> contracts = new java.util.ArrayList<>();
        String sql = "SELECT id, player_uuid, contract_id, accepted_at, expires_at, progress,"
                + " progress_state FROM player_active_contracts WHERE status = 'active'";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                ActiveContract contract = new ActiveContract(player, rs.getString("contract_id"),
                        instant(rs.getLong("accepted_at")), instant(rs.getLong("expires_at")));
                contract.setActiveId(rs.getInt("id"));
                contract.setProgress(rs.getDouble("progress"));
                contract.setProgressState(rs.getString("progress_state"));
                contracts.add(contract);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading active contracts: " + e.getMessage());
        }
        return contracts;
    }

    public boolean hasActiveContract(UUID playerUuid, String contractId) {        try (PreparedStatement stmt = connection.prepareStatement(
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

    /** Appends an influence row and credits/debits the alignment's running total. */
    public void logInfluence(String creditedAlignmentId, com.sunwayMinecraft.contracts.domain.InfluenceRecord record) {
        String insert = "INSERT INTO contract_influence_log"
                + " (contract_id, player_uuid, origin_alignment_id, destination_alignment_id,"
                + " origin_grand_alliance_id, destination_grand_alliance_id, influence_amount,"
                + " influence_type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, record.contractId());
            stmt.setString(2, record.playerUuid() == null ? null : record.playerUuid().toString());
            stmt.setString(3, record.originAlignmentId());
            stmt.setString(4, record.destinationAlignmentId());
            stmt.setString(5, record.originGrandAllianceId());
            stmt.setString(6, record.destinationGrandAllianceId());
            stmt.setInt(7, record.influenceAmount());
            stmt.setString(8, record.influenceType());
            stmt.setLong(9, record.createdAt().toEpochMilli());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error logging influence: " + e.getMessage());
        }
        if (creditedAlignmentId != null) {
            addAlignmentInfluence(creditedAlignmentId, record.influenceAmount());
        }
    }

    public void addAlignmentInfluence(String alignmentId, int delta) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO alignment_influence_totals (alignment_id, influence_total, updated_at)"
                        + " VALUES (?, ?, ?) ON CONFLICT(alignment_id) DO UPDATE SET"
                        + " influence_total = influence_total + excluded.influence_total,"
                        + " updated_at = excluded.updated_at")) {
            stmt.setString(1, alignmentId);
            stmt.setInt(2, delta);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating influence total: " + e.getMessage());
        }
    }

    public int getAlignmentInfluence(String alignmentId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT influence_total FROM alignment_influence_totals WHERE alignment_id = ?")) {
            stmt.setString(1, alignmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Total influence exchanged in either direction between two alignments. */
    public int getInfluenceBetween(String a, String b) {
        String sql = "SELECT COALESCE(SUM(influence_amount),0) FROM contract_influence_log"
                + " WHERE (origin_alignment_id = ? AND destination_alignment_id = ?)"
                + " OR (origin_alignment_id = ? AND destination_alignment_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, a);
            stmt.setString(2, b);
            stmt.setString(3, b);
            stmt.setString(4, a);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public java.util.List<com.sunwayMinecraft.contracts.domain.InfluenceRecord> getRecentInfluence(int limit) {
        java.util.List<com.sunwayMinecraft.contracts.domain.InfluenceRecord> rows =
                new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM contract_influence_log ORDER BY created_at DESC LIMIT ?")) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerUuid = rs.getString("player_uuid");
                    rows.add(new com.sunwayMinecraft.contracts.domain.InfluenceRecord(
                            rs.getLong("id"), rs.getString("contract_id"),
                            playerUuid == null ? null : UUID.fromString(playerUuid),
                            rs.getString("origin_alignment_id"), rs.getString("destination_alignment_id"),
                            rs.getString("origin_grand_alliance_id"), rs.getString("destination_grand_alliance_id"),
                            rs.getInt("influence_amount"), rs.getString("influence_type"),
                            instant(rs.getLong("created_at"))));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading recent influence: " + e.getMessage());
        }
        return rows;
    }

    public void addSupplyPoints(String alignmentId, int delta) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO alignment_supply_totals (alignment_id, supply_total, updated_at)"
                        + " VALUES (?, ?, ?) ON CONFLICT(alignment_id) DO UPDATE SET"
                        + " supply_total = supply_total + excluded.supply_total,"
                        + " updated_at = excluded.updated_at")) {
            stmt.setString(1, alignmentId);
            stmt.setInt(2, delta);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating supply total: " + e.getMessage());
        }
    }

    public int getSupplyPoints(String alignmentId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT supply_total FROM alignment_supply_totals WHERE alignment_id = ?")) {
            stmt.setString(1, alignmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public Map<String, Integer> getAllSupplyTotals() {
        Map<String, Integer> totals = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT alignment_id, supply_total FROM alignment_supply_totals");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                totals.put(rs.getString("alignment_id"), rs.getInt("supply_total"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading supply totals: " + e.getMessage());
        }
        return totals;
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

    /** Records a generated dynamic contract so it can be rebuilt on startup. */
    public void saveDynamicContract(String contractId, String templateId, long expiresAtMillis) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO contract_dynamic_contracts"
                        + " (contract_id, template_id, expires_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, contractId);
            stmt.setString(2, templateId);
            stmt.setLong(3, expiresAtMillis);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving dynamic contract: " + e.getMessage());
        }
    }

    /** Non-expired dynamic rows as (contractId, templateId, expiresAt). */
    public java.util.List<DynamicContractRow> loadDynamicContracts(long nowEpochMillis) {
        java.util.List<DynamicContractRow> rows = new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT contract_id, template_id, expires_at FROM contract_dynamic_contracts"
                        + " WHERE expires_at >= ?")) {
            stmt.setLong(1, nowEpochMillis);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DynamicContractRow(rs.getString("contract_id"),
                            rs.getString("template_id"), rs.getLong("expires_at")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading dynamic contracts: " + e.getMessage());
        }
        return rows;
    }

    /** Drops dynamic rows whose expiry has passed; returns how many were removed. */
    public int purgeExpiredDynamicContracts(long nowEpochMillis) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM contract_dynamic_contracts WHERE expires_at < ?")) {
            stmt.setLong(1, nowEpochMillis);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error purging dynamic contracts: " + e.getMessage());
            return 0;
        }
    }

    public void deleteDynamicContract(String contractId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM contract_dynamic_contracts WHERE contract_id = ?")) {
            stmt.setString(1, contractId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting dynamic contract: " + e.getMessage());
        }
    }

    /** A persisted dynamic-contract row: generated id, source template and expiry. */
    public record DynamicContractRow(String contractId, String templateId, long expiresAtMillis) {}

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
