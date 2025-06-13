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
}
