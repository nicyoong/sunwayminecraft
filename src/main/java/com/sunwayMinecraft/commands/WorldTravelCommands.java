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
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    String commandName = command.getName().toLowerCase();

    switch (commandName) {
      case "mineworld":
        return handleMining(player);
      case "lifeworld":
        return handleLiving(player);
      default:
        return false;
    }
  }

  private boolean handleMining(Player player) {
    if (worldTravelManager.isMiningWorld(player)) {
      player.sendMessage(
              Component.text("You are already in the Mining World.", NamedTextColor.YELLOW));
      return true;
    }

    return worldTravelManager.teleportToMining(player);
  }

  private boolean handleLiving(Player player) {
    if (worldTravelManager.isLivingWorld(player) && player.getRespawnLocation() == null) {
      player.sendMessage(
              Component.text("You are already in the Living World and have no personal spawn point set.", NamedTextColor.YELLOW));
      return true;
    }

    return worldTravelManager.teleportToLiving(player);
  }
}