package com.sunwayMinecraft.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorldTravelCommands implements CommandExecutor {

  private static final String MINING_WORLD = "mining";
  private static final String LIVING_WORLD = "world";

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    String commandName = command.getName().toLowerCase();

    return switch (commandName) {
      case "mining" -> {
        teleportToWorld(player, MINING_WORLD, "Mining World");
        yield true;
      }
      case "living" -> {
        teleportToWorld(player, LIVING_WORLD, "Living World");
        yield true;
      }
      default -> false;
    };
  }

  private void teleportToWorld(Player player, String worldName, String displayName) {
    World targetWorld = Bukkit.getWorld(worldName);

    if (targetWorld == null) {
      player.sendMessage(
          Component.text("That world is not available right now.", NamedTextColor.RED)
      );
      return;
    }

    boolean success = player.teleport(targetWorld.getSpawnLocation());

    if (!success) {
      player.sendMessage(
          Component.text("Teleport failed. Please try again.", NamedTextColor.RED)
      );
      return;
    }

    player.sendMessage(
        Component.text("Teleported to ", NamedTextColor.GREEN)
            .append(Component.text(displayName, NamedTextColor.GOLD))
            .append(Component.text(".", NamedTextColor.GREEN))
    );
  }
}