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
}
