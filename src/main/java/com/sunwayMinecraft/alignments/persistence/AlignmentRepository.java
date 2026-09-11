package com.sunwayMinecraft.alignments.persistence;

import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Level;

/** SQLite persistence for player alignment memberships (alignments.db). */
public class AlignmentRepository {
  private final JavaPlugin plugin;
  private Connection connection;

  public AlignmentRepository(JavaPlugin plugin) {
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
          .log(Level.SEVERE, "Failed to initialize alignment database: " + e.getMessage(), e);
    }
  }

  private void createTables() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      String sql =
          "CREATE TABLE IF NOT EXISTS player_alignment_membership ("
              + "player_uuid TEXT PRIMARY KEY,"
              + "alignment_id TEXT NOT NULL,"
              + "joined_at INTEGER NOT NULL,"
              + "reputation INTEGER NOT NULL DEFAULT 0,"
              + "status TEXT NOT NULL DEFAULT 'active')";
      stmt.execute(sql);

      String cooldownSql =
          "CREATE TABLE IF NOT EXISTS player_alignment_cooldown ("
              + "player_uuid TEXT PRIMARY KEY,"
              + "last_switch_at INTEGER NOT NULL)";
      stmt.execute(cooldownSql);
    }
  }

  /** True when the database connection and schema are usable. */
  public boolean isAvailable() {
    return connection != null;
  }

  /** Inserts or replaces the player's membership. Returns false on failure. */
  public boolean upsert(AlignmentMembership membership) {
    if (!isAvailable()) return false;
    String sql =
        "INSERT INTO player_alignment_membership "
            + "(player_uuid, alignment_id, joined_at, reputation, status) "
            + "VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT(player_uuid) DO UPDATE SET "
            + "alignment_id = excluded.alignment_id, "
            + "joined_at = excluded.joined_at, "
            + "reputation = excluded.reputation, "
            + "status = excluded.status";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, membership.playerUuid().toString());
      pstmt.setString(2, membership.alignmentId());
      pstmt.setLong(3, membership.joinedAt());
      pstmt.setInt(4, membership.reputation());
      pstmt.setString(5, membership.status());
      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error saving alignment membership: " + e.getMessage());
      return false;
    }
  }

  /** Removes the player's membership. Returns false on failure. */
  public boolean remove(UUID playerUuid) {
    if (!isAvailable()) return false;
    String sql = "DELETE FROM player_alignment_membership WHERE player_uuid = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, playerUuid.toString());
      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error removing alignment membership: " + e.getMessage());
      return false;
    }
  }

  public Optional<AlignmentMembership> findByUuid(UUID playerUuid) {
    if (!isAvailable()) return Optional.empty();
    String sql =
        "SELECT alignment_id, joined_at, reputation, status "
            + "FROM player_alignment_membership WHERE player_uuid = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, playerUuid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new AlignmentMembership(
                  playerUuid,
                  rs.getString("alignment_id"),
                  rs.getLong("joined_at"),
                  rs.getInt("reputation"),
                  rs.getString("status")));
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading alignment membership: " + e.getMessage());
      return Optional.empty();
    }
  }

  /** Records when a player last joined/switched alignment, for cooldowns. */
  public boolean setLastSwitchAt(UUID playerUuid, long epochMillis) {
    if (!isAvailable()) return false;
    String sql =
        "INSERT INTO player_alignment_cooldown (player_uuid, last_switch_at) VALUES (?, ?) "
            + "ON CONFLICT(player_uuid) DO UPDATE SET last_switch_at = excluded.last_switch_at";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, playerUuid.toString());
      pstmt.setLong(2, epochMillis);
      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      plugin.getLogger().severe("Error saving alignment switch cooldown: " + e.getMessage());
      return false;
    }
  }

  /** Epoch millis of the player's last alignment switch, or empty if none. */
  public OptionalLong getLastSwitchAt(UUID playerUuid) {
    if (!isAvailable()) return OptionalLong.empty();
    String sql = "SELECT last_switch_at FROM player_alignment_cooldown WHERE player_uuid = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, playerUuid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return OptionalLong.of(rs.getLong("last_switch_at"));
        }
      }
      return OptionalLong.empty();
    } catch (SQLException e) {
      plugin.getLogger().severe("Error loading alignment switch cooldown: " + e.getMessage());
      return OptionalLong.empty();
    }
  }

  /**
   * Member count per alignment id, including offline players. Returns an
   * empty map when the database is unavailable rather than failing callers.
   */
  public Map<String, Integer> countByAlignment() {
    Map<String, Integer> counts = new HashMap<>();
    if (!isAvailable()) return counts;
    String sql =
        "SELECT alignment_id, COUNT(*) AS members FROM player_alignment_membership "
            + "GROUP BY alignment_id";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        counts.put(rs.getString("alignment_id"), rs.getInt("members"));
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error counting alignment members: " + e.getMessage());
      return Map.of();
    }
    return counts;
  }

  public void close() {
    try {
      if (connection != null) {
        connection.close();
        connection = null;
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error closing alignment database: " + e.getMessage());
    }
  }
}
