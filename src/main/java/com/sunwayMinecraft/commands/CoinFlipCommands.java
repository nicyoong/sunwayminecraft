package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.coinflip.CoinFlipSystem;
import com.sunwayMinecraft.coinflip.ItemCoinFlipSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CoinFlipCommands implements CommandExecutor, TabCompleter {
  private final CoinFlipSystem coinFlipSystem;
  private final ItemCoinFlipSystem itemCoinFlipSystem;

  public CoinFlipCommands(CoinFlipSystem coinFlipSystem, ItemCoinFlipSystem itemCoinFlipSystem) {
    this.coinFlipSystem = coinFlipSystem;
    this.itemCoinFlipSystem = itemCoinFlipSystem;
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
    }

    // Default to money flip
    handleMoneyFlip(player, args);
    return true;
  }

  private void handleMoneyFlip(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage("§cUsage: /cf <amount> <heads|h|tails|t>");
      return;
    }

    try {
      double amount = Double.parseDouble(args[0]);
      if (amount <= 0) throw new NumberFormatException();

      String sideInput = args[1].toLowerCase();
      boolean isHeads = sideInput.startsWith("h");
      boolean isTails = sideInput.startsWith("t");

      if (!isHeads && !isTails) {
        player.sendMessage("§cInvalid side! Use heads/h or tails/t");
        return;
      }

      coinFlipSystem.processCoinFlip(player, amount, isHeads);
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

      String sideInput = args[2].toLowerCase();
      boolean isHeads = sideInput.startsWith("h");
      boolean isTails = sideInput.startsWith("t");

      if (!isHeads && !isTails) {
        player.sendMessage("§cInvalid side! Use heads/h or tails/t");
        return;
      }

      itemCoinFlipSystem.processItemFlip(player, amount, isHeads);
    } catch (NumberFormatException e) {
      player.sendMessage("§cInvalid amount! Must be a positive integer.");
    }
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
  public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
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
    } else if (args.length == 3) {
      if ("item".equalsIgnoreCase(args[0])) {
        completions.add("heads");
        completions.add("h");
        completions.add("tails");
        completions.add("t");
      } else if (!args[0].matches("(?i)help|mute|unmute")) {
        completions.add("heads");
        completions.add("h");
        completions.add("tails");
        completions.add("t");
      }
    }
    return completions;
  }
}