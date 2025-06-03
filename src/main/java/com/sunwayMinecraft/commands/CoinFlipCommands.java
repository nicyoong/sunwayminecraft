package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.coinflip.CoinFlipSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CoinFlipCommands implements CommandExecutor, TabCompleter {
  private final CoinFlipSystem coinFlipSystem;

  public CoinFlipCommands(CoinFlipSystem coinFlipSystem) {
    this.coinFlipSystem = coinFlipSystem;
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

    switch (args[0].toLowerCase()) {
      case "mute":
        coinFlipSystem.handleMute(player, true);
        return true;
      case "unmute":
        coinFlipSystem.handleMute(player, false);
        return true;
      case "help":
        sendHelp(player);
        return true;
    }

    // Coin flip logic
    if (args.length < 2) {
      player.sendMessage("§cUsage: /cf <amount> <heads|tails|h|t>");
      return true;
    }

    try {
      double amount = Double.parseDouble(args[0]);
      if (amount <= 0) throw new NumberFormatException();

      String sideInput = args[1].toLowerCase();
      boolean isHeads = sideInput.startsWith("h");
      boolean isTails = sideInput.startsWith("t");

      if (!isHeads && !isTails) {
        player.sendMessage("§cInvalid side! Use heads/h or tails/t");
        return true;
      }

      coinFlipSystem.processCoinFlip(player, amount, isHeads);
    } catch (NumberFormatException e) {
      player.sendMessage("§cInvalid amount! Must be a positive number.");
    }
    return true;
  }

  private void sendHelp(Player player) {
    player.sendMessage("§6--- Coin Flip Help ---");
    player.sendMessage("§e/cf <amount> <heads|h|tails|t>§f - Place a bet");
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
      completions.add("10");
      completions.add("50");
      completions.add("100");
    } else if (args.length == 2 && !args[0].matches("(?i)help|mute|unmute")) {
      completions.add("heads");
      completions.add("h");
      completions.add("tails");
      completions.add("t");
    }
    return completions;
  }
}