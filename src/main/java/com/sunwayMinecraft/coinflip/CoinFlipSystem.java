package com.sunwayMinecraft.coinflip;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CoinFlipSystem {
  private final Economy econ;
  private final Set<UUID> mutedPlayers = new HashSet<>();
  private final Random random = new Random();

  public CoinFlipSystem(Economy econ) {
    this.econ = econ;
  }

  public void processCoinFlip(Player player, double amount, boolean playerGuessHeads) {
    if (econ.getBalance(player) < amount) {
      player.sendMessage("§cInsufficient funds!");
      return;
    }

    econ.withdrawPlayer(player, amount);
    boolean won = processFlipLogic(playerGuessHeads);

    if (won) econ.depositPlayer(player, amount * 2);
    sendMoneyResult(player, won, amount);
  }

  // Reusable flip logic
  public boolean processFlipLogic(boolean playerGuessHeads) {
    player.sendMessage("§aYou bet " + (playerGuessHeads ? "heads" : "tails"));
    boolean isHeads = random.nextBoolean();
    return playerGuessHeads == isHeads;
  }

  private void sendMoneyResult(Player player, boolean won, double amount) {
    if (isMuted(player)) return;

    String result = won ?
            "§aYou won §e" + econ.format(amount) :
            "§cYou lost §e" + econ.format(amount);

    player.sendMessage(result);
  }

  public void handleMute(Player player, boolean mute) {
    if (mute) {
      mutedPlayers.add(player.getUniqueId());
      player.sendMessage("§6Coin flip messages muted."); // §6 = gold
    } else {
      mutedPlayers.remove(player.getUniqueId());
      player.sendMessage("§6Coin flip messages unmuted.");
    }
  }

  public boolean isMuted(Player player) {
    return mutedPlayers.contains(player.getUniqueId());
  }
}