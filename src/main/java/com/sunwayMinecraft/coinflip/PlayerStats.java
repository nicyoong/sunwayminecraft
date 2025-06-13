package com.sunwayMinecraft.coinflip;

import java.util.UUID;

public class PlayerStats {
  private final UUID uuid;
  private int moneyWins;
  private int moneyLosses;
  private double moneyWagered;
  private double moneyWon;
  private int itemWins;
  private int itemLosses;
  private int itemsWagered;
  private int itemsWon;

  public PlayerStats(UUID uuid) {
    this.uuid = uuid;
  }

  // Getters
  public UUID getUuid() {
    return uuid;
  }

  public int getMoneyWins() {
    return moneyWins;
  }

  public int getMoneyLosses() {
    return moneyLosses;
  }

  public double getMoneyWagered() {
    return moneyWagered;
  }

  public double getMoneyWon() {
    return moneyWon;
  }

  public int getItemWins() {
    return itemWins;
  }

  public int getItemLosses() {
    return itemLosses;
  }

  public int getItemsWagered() {
    return itemsWagered;
  }

  public int getItemsWon() {
    return itemsWon;
  }

  // Setters
  public void addMoneyWin(double amount) {
    moneyWins++;
    moneyWon += amount * 2;
    moneyWagered += amount;
  }

  public void addMoneyLoss(double amount) {
    moneyLosses++;
    moneyWagered += amount;
  }

  public void addItemWin(int amount) {
    itemWins++;
    itemsWon += amount * 2;
    itemsWagered += amount;
  }

  public void addItemLoss(int amount) {
    itemLosses++;
    itemsWagered += amount;
  }

  // Calculated properties
  public int getTotalGames() {
    return moneyWins + moneyLosses + itemWins + itemLosses;
  }

  public double getWinPercentage() {
    int wins = moneyWins + itemWins;
    int total = getTotalGames();
    return total > 0 ? (double) wins / total * 100 : 0;
  }

  public double getMoneyProfit() {
    return moneyWon - moneyWagered;
  }
}
