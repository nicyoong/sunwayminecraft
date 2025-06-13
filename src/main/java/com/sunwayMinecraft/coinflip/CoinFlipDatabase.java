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
}
