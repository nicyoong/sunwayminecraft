package com.sunwayMinecraft.coinflip;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;

public class CoinFlipDatabase {
  private final JavaPlugin plugin;
  private Connection connection;

  public CoinFlipDatabase(JavaPlugin plugin) {
    this.plugin = plugin;
    initializeDatabase();
  }

  private void initializeDatabase() {
    try {
      Class.forName("org.sqlite.JDBC");
      connection =
          DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/coinflip.db");
      createTables();
    } catch (ClassNotFoundException | SQLException e) {
      plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
    }
  }

  private void createTables() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      String sql = "CREATE TABLE IF NOT EXISTS player_stats ("
              + "uuid TEXT PRIMARY KEY,"
              + "money_wins INTEGER DEFAULT 0,"
              + "money_losses INTEGER DEFAULT 0,"
              + "money_wagered REAL DEFAULT 0,"
              + "money_won REAL DEFAULT 0,"
              + "item_wins INTEGER DEFAULT 0,"
              + "item_losses INTEGER DEFAULT 0,"
              + "items_wagered INTEGER DEFAULT 0,"
              + "items_won INTEGER DEFAULT 0)";
      stmt.execute(sql);
    }
  }

  public PlayerStats getPlayerStats(UUID uuid) {
    PlayerStats stats = new PlayerStats(uuid);
    String sql = "SELECT * FROM player_stats WHERE uuid = ?";

    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, uuid.toString());
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        stats = new PlayerStats(uuid);
        stats.moneyWins = rs.getInt("money_wins");
        stats.moneyLosses = rs.getInt("money_losses");
        stats.moneyWagered = rs.getDouble("money_wagered");
        stats.moneyWon = rs.getDouble("money_won");
        stats.itemWins = rs.getInt("item_wins");
        stats.itemLosses = rs.getInt("item_losses");
        stats.itemsWagered = rs.getInt("items_wagered");
        stats.itemsWon = rs.getInt("items_won");
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Error getting player stats: " + e.getMessage());
    }
    return stats;
  }

  public void updateStats(PlayerStats stats) {
    String sql = "INSERT OR REPLACE INTO player_stats ("
            + "uuid, money_wins, money_losses, money_wagered, money_won, "
            + "item_wins, item_losses, items_wagered, items_won) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, stats.getUuid().toString());
      pstmt.setInt(2, stats.getMoneyWins());
      pstmt.setInt(3, stats.getMoneyLosses());
      pstmt.setDouble(4, stats.getMoneyWagered());
      pstmt.setDouble(5, stats.getMoneyWon());
      pstmt.setInt(6, stats.getItemWins());
      pstmt.setInt(7, stats.getItemLosses());
      pstmt.setInt(8, stats.getItemsWagered());
      pstmt.setInt(9, stats.getItemsWon());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      plugin.getLogger().severe("Error updating player stats: " + e.getMessage());
    }
  }
}
