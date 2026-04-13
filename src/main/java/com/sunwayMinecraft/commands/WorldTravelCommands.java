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
      case "mining":
        worldTravelManager.teleportToMining(player);
        return true;

      case "living":
        worldTravelManager.teleportToLiving(player);
        return true;

      default:
        player.sendMessage(
                Component.text("Unknown travel command.", NamedTextColor.RED));
        return true;
    }
  }
}