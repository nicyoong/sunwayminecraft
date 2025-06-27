package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.regions.RegionManager;
import com.sunwayMinecraft.switches.SwitchConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.Map;

public class SwitchesCommands implements CommandExecutor {

  private final JavaPlugin plugin;
  private final SwitchConfigManager switchConfig;
  private final RegionManager regionManager;

  public SwitchesCommands(
          JavaPlugin plugin,
          SwitchConfigManager switchConfig,
          RegionManager regionManager
  ) {
    this.plugin = plugin;
    this.switchConfig = switchConfig;
    this.regionManager = regionManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    switch (cmd.getName().toLowerCase()) {
      case "scanlights": return handleScanLights(sender, args);
      case "exportlights": return handleExportLights(sender);
      case "reloadsunwayswitches": return handleReloadSwitches(sender);
      default: return false;
    }
  }

  private boolean handleScanLights(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can use this command");
      return true;
    }

    Player player = (Player) sender;
    Location loc = player.getLocation();
    player.sendMessage("§eLight scanning is no longer needed. Use region commands instead:");
    player.sendMessage("§6/sunwayregioncreate <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
    player.sendMessage("§7Your current position: §b" + loc.getWorld().getName() +
            "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    return true;
  }

  private boolean handleExportLights(CommandSender sender) {
    sender.sendMessage("§eLight exporting is no longer needed. Regions are now managed in the database.");
    sender.sendMessage("§6Use §b/sunwayregionlist §6to view all regions");
    return true;
  }

  private boolean handleReloadSwitches(CommandSender sender) {
    if (!sender.hasPermission("sunwayminecraft.switch.reload")) {
      sender.sendMessage("§cYou don't have permission!");
      return true;
    }

    switchConfig.reload();
    sender.sendMessage("§aSwitch configuration reloaded successfully.");
    return true;
  }
}