package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.worldtravel.WorldTravelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorldTravelCommands implements CommandExecutor {

  private final WorldTravelManager worldTravelManager;

  public WorldTravelCommands(WorldTravelManager worldTravelManager) {
    this.worldTravelManager = worldTravelManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String commandName = command.getName().toLowerCase();

    if (commandName.equals("mininginfo")) {
      sender.sendMessage(worldTravelManager.buildMiningInfoMessage());
      return true;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    return switch (commandName) {
      case "mineworld" -> handleMineWorld(player);
      case "lifeworld" -> handleLifeWorld(player);
      default -> false;
    };
  }

  private boolean handleMineWorld(Player player) {
    if (worldTravelManager.isMiningWorld(player)) {
      player.sendMessage(
              Component.text("You are already in the Mining World.", NamedTextColor.YELLOW));
      return true;
    }

    return worldTravelManager.teleportToMining(player);
  }

  private boolean handleLifeWorld(Player player) {
    return worldTravelManager.teleportToLifeWorld(player);
  }
}