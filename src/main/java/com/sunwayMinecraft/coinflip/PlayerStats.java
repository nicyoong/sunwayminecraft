package com.sunwayMinecraft.coinflip;

import java.util.UUID;

public class PlayerStats {
  public final UUID uuid;
  public int moneyWins;
  public int moneyLosses;
  public double moneyWagered;
  public double moneyWon;
  public int itemWins;
  public int itemLosses;
  public int itemsWagered;
  public int itemsWon;

  public PlayerStats(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getUuid() {
    return uuid;
  }

  public int getMoneyWins() {
    return moneyWins;
  }

  public void setMoneyWins(int moneyWins) {
    this.moneyWins = moneyWins;
  }

  public int getMoneyLosses() {
    return moneyLosses;
  }

  public void setMoneyLosses(int moneyLosses) {
    this.moneyLosses = moneyLosses;
  }

  public double getMoneyWagered() {
    return moneyWagered;
  }

  public void setMoneyWagered(double moneyWagered) {
    this.moneyWagered = moneyWagered;
  }

  public double getMoneyWon() {
    return moneyWon;
  }

  public void setMoneyWon(double moneyWon) {
    this.moneyWon = moneyWon;
  }

  public int getItemWins() {
    return itemWins;
  }

  public void setItemWins(int itemWins) {
    this.itemWins = itemWins;
  }

  public int getItemLosses() {
    return itemLosses;
  }

  public void setItemLosses(int itemLosses) {
    this.itemLosses = itemLosses;
  }

  public int getItemsWagered() {
    return itemsWagered;
  }

  public void setItemsWagered(int itemsWagered) {
    this.itemsWagered = itemsWagered;
  }

  public int getItemsWon() {
    return itemsWon;
  }

  public void setItemsWon(int itemsWon) {
    this.itemsWon = itemsWon;
  }

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
