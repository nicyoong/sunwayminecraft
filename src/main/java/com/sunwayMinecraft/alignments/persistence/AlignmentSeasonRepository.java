package com.sunwayMinecraft.alignments.persistence;

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
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite persistence for seasons: per-alignment snapshots, per-player
 * reputation records and the current season state. Uses its own connection
 * to alignments.db; snapshots are written once per season end.
 */
public class AlignmentSeasonRepository {
  /** Totals for one alignment at snapshot time. */
  public record AlignmentSnapshot(
      long id, String seasonId, String alignmentId, String grandAllianceId,
      long totalReputation, int memberCount, long snapshotAt) {}

  /** One player's reputation record inside a season. */
  public record PlayerSeasonRecord(
      String seasonId, UUID playerUuid, String alignmentId, int reputation, long recordedAt) {}

  /** Single-row current season state. */
  public record SeasonState(String seasonId, long startedAt) {}

  private final JavaPlugin plugin;
  private Connection connection;

  public AlignmentSeasonRepository(JavaPlugin plugin) {
    this.plugin = plugin;
    initializeDatabase();
  }

  private void initializeDatabase() {
    try {
      Class.forName("org.sqlite.JDBC");
      connection =
          DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/alignments.db");
      createTables();
    } catch (ClassNotFoundException | SQLException e) {
      connection = null;
      plugin.getLogger()
          .log(Level.SEVERE, "Failed to initialize alignment season database: " + e.getMessage(), e);
    }
  }

  private void createTables() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS alignment_season_snapshots ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "season_id TEXT NOT NULL,"
              + "alignment_id TEXT NOT NULL,"
              + "grand_alliance_id TEXT NOT NULL,"
              + "total_reputation INTEGER NOT NULL,"
              + "member_count INTEGER NOT NULL,"
              + "snapshot_at INTEGER NOT NULL)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS player_season_reputation ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "season_id TEXT NOT NULL,"
              + "player_uuid TEXT NOT NULL,"
              + "alignment_id TEXT NOT NULL,"
              + "reputation INTEGER NOT NULL,"
              + "recorded_at INTEGER NOT NULL)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS alignment_season_state ("
              + "id INTEGER PRIMARY KEY CHECK (id = 1),"
              + "season_id TEXT NOT NULL,"
              + "started_at INTEGER NOT NULL)");
    }
  }

  public boolean isAvailable() {
    return connection != null;
  }

  public Optional<SeasonState> getSeasonState() {
    if (!isAvailable()) return Optional.empty();
    String sql = "SELECT season_id, started_at FROM alignment_season_state WHERE id = 1";
    try (PreparedStatement pstmt = connection.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
      if (rs.next()) {
        return Optional.of(new SeasonState(rs.getString("season_id"), rs.getLong("started_at")));
      }
      return Optional.empty();
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading season state: " + e.getMessage());
      return Optional.empty();
    }
  }

  /** Inserts or replaces the single season state row. */
  public boolean saveSeasonState(SeasonState state) {
    if (!isAvailable()) return false;
    String sql =
        "INSERT INTO alignment_season_state (id, season_id, started_at) VALUES (1, ?, ?) "
            + "ON CONFLICT(id) DO UPDATE SET season_id = excluded.season_id, "
            + "started_at = excluded.started_at";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, state.seasonId());
      pstmt.setLong(2, state.startedAt());
      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error saving season state: " + e.getMessage());
      return false;
    }
  }

  public boolean insertSnapshots(List<AlignmentSnapshot> snapshots) {
    if (!isAvailable()) return false;
    String sql =
        "INSERT INTO alignment_season_snapshots "
            + "(season_id, alignment_id, grand_alliance_id, total_reputation, member_count, snapshot_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      for (AlignmentSnapshot snapshot : snapshots) {
        pstmt.setString(1, snapshot.seasonId());
        pstmt.setString(2, snapshot.alignmentId());
        pstmt.setString(3, snapshot.grandAllianceId());
        pstmt.setLong(4, snapshot.totalReputation());
        pstmt.setInt(5, snapshot.memberCount());
        pstmt.setLong(6, snapshot.snapshotAt());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error inserting season snapshots: " + e.getMessage());
      return false;
    }
  }

  public boolean insertPlayerRecords(List<PlayerSeasonRecord> records) {
    if (!isAvailable()) return false;
    String sql =
        "INSERT INTO player_season_reputation "
            + "(season_id, player_uuid, alignment_id, reputation, recorded_at) "
            + "VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      for (PlayerSeasonRecord record : records) {
        pstmt.setString(1, record.seasonId());
        pstmt.setString(2, record.playerUuid().toString());
        pstmt.setString(3, record.alignmentId());
        pstmt.setInt(4, record.reputation());
        pstmt.setLong(5, record.recordedAt());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error inserting player season records: " + e.getMessage());
      return false;
    }
  }

  /**
   * Snapshots of the most recent snapshot run: for each alignment the rows
   * with the highest snapshot_at.
   */
  public List<AlignmentSnapshot> getLatestSnapshots() {
    List<AlignmentSnapshot> snapshots = new ArrayList<>();
    if (!isAvailable()) return snapshots;
    String sql =
        "SELECT s.* FROM alignment_season_snapshots s "
            + "JOIN (SELECT alignment_id, MAX(snapshot_at) AS latest "
            + "      FROM alignment_season_snapshots GROUP BY alignment_id) m "
            + "ON s.alignment_id = m.alignment_id AND s.snapshot_at = m.latest "
            + "ORDER BY s.total_reputation DESC";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        snapshots.add(new AlignmentSnapshot(
            rs.getLong("id"), rs.getString("season_id"), rs.getString("alignment_id"),
            rs.getString("grand_alliance_id"), rs.getLong("total_reputation"),
            rs.getInt("member_count"), rs.getLong("snapshot_at")));
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading latest snapshots: " + e.getMessage());
    }
    return snapshots;
  }

  /** Reputation totals per alignment for one season, from player records. */
  public Map<String, long[]> getSeasonAlignmentTotals(String seasonId) {
    Map<String, long[]> totals = new HashMap<>();
    if (!isAvailable()) return totals;
    String sql =
        "SELECT alignment_id, SUM(reputation) AS total_rep, COUNT(*) AS members "
            + "FROM player_season_reputation WHERE season_id = ? GROUP BY alignment_id";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, seasonId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          totals.put(rs.getString("alignment_id"),
              new long[] {rs.getLong("total_rep"), rs.getLong("members")});
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading season totals: " + e.getMessage());
      return Map.of();
    }
    return totals;
  }

  /** A player's recorded reputation across seasons, oldest first. */
  public List<PlayerSeasonRecord> getPlayerSeasonHistory(UUID playerUuid) {
    List<PlayerSeasonRecord> history = new ArrayList<>();
    if (!isAvailable()) return history;
    String sql =
        "SELECT season_id, alignment_id, reputation, recorded_at "
            + "FROM player_season_reputation WHERE player_uuid = ? ORDER BY recorded_at";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, playerUuid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          history.add(new PlayerSeasonRecord(
              rs.getString("season_id"), playerUuid, rs.getString("alignment_id"),
              rs.getInt("reputation"), rs.getLong("recorded_at")));
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading player season history: " + e.getMessage());
    }
    return history;
  }

  /** Resets every membership's reputation to zero; returns rows affected. */
  public int resetAllReputations() {
    if (!isAvailable()) return 0;
    try (Statement stmt = connection.createStatement()) {
      return stmt.executeUpdate("UPDATE player_alignment_membership SET reputation = 0");
    } catch (SQLException e) {
      plugin.getLogger().severe("Error resetting reputations: " + e.getMessage());
      return 0;
    }
  }

  public void close() {
    try {
      if (connection != null) {
        connection.close();
        connection = null;
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error closing season database: " + e.getMessage());
    }
  }
}
