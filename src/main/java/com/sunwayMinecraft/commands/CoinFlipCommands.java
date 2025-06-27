package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.coinflip.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CoinFlipCommands implements CommandExecutor, TabCompleter {
  private static final Set<String> HEADS_ALIASES = Set.of("heads", "h");
  private static final Set<String> TAILS_ALIASES = Set.of("tails", "t");
  private final CoinFlipSystem coinFlipSystem;
  private final ItemCoinFlipSystem itemCoinFlipSystem;
  private final CoinFlipDatabase database;
  private final Economy econ;

  public CoinFlipCommands(
      CoinFlipSystem coinFlipSystem,
      ItemCoinFlipSystem itemCoinFlipSystem,
      CoinFlipDatabase database) {
    this.coinFlipSystem = coinFlipSystem;
    this.itemCoinFlipSystem = itemCoinFlipSystem;
    this.database = database;
    this.econ = coinFlipSystem.getEconomy();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can use this command!");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      sendHelp(player);
      return true;
    }

    String subCommand = args[0].toLowerCase();
    switch (subCommand) {
      case "mute":
        coinFlipSystem.handleMute(player, true);
        return true;
      case "unmute":
        coinFlipSystem.handleMute(player, false);
        return true;
      case "help":
        sendHelp(player);
        return true;
      case "item":
        handleItemFlip(player, args);
        return true;
      case "stats":
        showStats(player);
        return true;
      default:
        handleMoneyFlip(player, args);
        return true;
    }
  }

  private void handleMoneyFlip(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage("§cUsage: /cf <amount> <heads|h|tails|t>");
      return;
    }

    try {
      double amount = Double.parseDouble(args[0]);
      if (amount <= 0) throw new NumberFormatException();

      // Validate side argument
      String sideInput = args[1].toLowerCase();
      Optional<Boolean> sideOpt = parseSide(sideInput);
      if (sideOpt.isEmpty()) {
        player.sendMessage("§cInvalid side! Use heads/h or tails/t");
        return;
      }

      coinFlipSystem.processCoinFlip(player, amount, sideOpt.get());
    } catch (NumberFormatException e) {
      player.sendMessage("§cInvalid amount! Must be a positive number.");
    }
  }

  private void handleItemFlip(Player player, String[] args) {
    if (args.length < 3) {
      player.sendMessage("§cUsage: /cf item <amount> <heads|h|tails|t>");
      return;
    }

    try {
      int amount = Integer.parseInt(args[1]);
      if (amount <= 0) throw new NumberFormatException();

      // Validate side argument
      String sideInput = args[2].toLowerCase();
      Optional<Boolean> sideOpt = parseSide(sideInput);
      if (sideOpt.isEmpty()) {
        player.sendMessage("§cInvalid side! Use heads/h or tails/t");
        return;
      }

      itemCoinFlipSystem.processItemFlip(player, amount, sideOpt.get());
    } catch (NumberFormatException e) {
      player.sendMessage("§cInvalid amount! Must be a positive integer.");
    }
  }

  private Optional<Boolean> parseSide(String input) {
    if (HEADS_ALIASES.contains(input)) return Optional.of(true);
    if (TAILS_ALIASES.contains(input)) return Optional.of(false);
    return Optional.empty();
  }

  private void sendHelp(Player player) {
    player.sendMessage("§6--- Coin Flip Help ---");
    player.sendMessage("§e/cf <amount> <heads|h|tails|t>§f - Place a bet");
    player.sendMessage("§e/cf item <amount> <heads|h|tails|t> §f- Place an item bet");
    player.sendMessage("§e/cf mute§f - Silence coin flip messages");
    player.sendMessage("§e/cf unmute§f - Enable coin flip messages");
    player.sendMessage("§e/cf help§f - Show this help");
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command cmd, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.add("help");
      completions.add("mute");
      completions.add("unmute");
      completions.add("item");
      completions.add("10");
      completions.add("50");
      completions.add("100");
    } else if (args.length == 2) {
      if ("item".equalsIgnoreCase(args[0])) {
        completions.add("1");
        completions.add("16");
        completions.add("32");
        completions.add("64");
      } else if (!args[0].matches("(?i)help|mute|unmute")) {
        completions.add("10");
        completions.add("50");
        completions.add("100");
      }
    } else if (args.length == 3
            && ("item".equalsIgnoreCase(args[0])
            || !args[0].matches("(?i)help|mute|unmute"))) {
      completions.addAll(HEADS_ALIASES);
      completions.addAll(TAILS_ALIASES);
    }
    return completions;
  }

  private void showStats(Player player) {
    PlayerStats stats = database.getPlayerStats(player.getUniqueId());

    player.sendMessage("§6=== Coin Flip Stats ===");
    player.sendMessage("§eTotal Games: §f" + stats.getTotalGames());
    player.sendMessage("§eWin Rate: §f" + String.format("%.2f", stats.getWinPercentage()) + "%");
    player.sendMessage("");
    player.sendMessage("§6Money Flips:");
    player.sendMessage("§7- Wins: §a" + stats.getMoneyWins());
    player.sendMessage("§7- Losses: §c" + stats.getMoneyLosses());
    player.sendMessage("§7- Wagered: §e" + econ.format(stats.getMoneyWagered()));
    player.sendMessage("§7- Won: §a" + econ.format(stats.getMoneyWon()));
    player.sendMessage(
        "§7- Profit: §"
            + (stats.getMoneyProfit() >= 0 ? "a" : "c")
            + econ.format(stats.getMoneyProfit()));
    player.sendMessage("");
    player.sendMessage("§6Item Flips:");
    player.sendMessage("§7- Wins: §a" + stats.getItemWins());
    player.sendMessage("§7- Losses: §c" + stats.getItemLosses());
    player.sendMessage("§7- Items Wagered: §e" + stats.getItemsWagered());
    player.sendMessage("§7- Items Won: §a" + stats.getItemsWon());
    player.sendMessage(
        "§7- Net Items: §"
            + (stats.getItemsWon() - stats.getItemsWagered() >= 0 ? "a" : "c")
            + (stats.getItemsWon() - stats.getItemsWagered()));
  }
}
