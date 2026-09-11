package com.sunwayMinecraft.alignments.persistence;

import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
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
