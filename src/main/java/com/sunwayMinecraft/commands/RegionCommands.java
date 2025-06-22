package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.regions.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class RegionCommands {
    private final JavaPlugin plugin;
    private final RegionManager regionManager;

    public RegionCommands(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                return createRegion(sender, args);
            case "resize":
                return resizeRegion(sender, args);
            case "delete":
                return deleteRegion(sender, args);
            case "decouple":
                return decoupleRegion(sender, args);
            case "trust":
                return manageTrust(sender, args, true);
            case "untrust":
                return manageTrust(sender, args, false);
            case "trustlist":
                return listTrust(sender, args);
            case "list":
                return listRegions(sender);
            case "here":
                return listRegionsAt(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand");
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eRegion Commands:");
        sender.sendMessage("§6/sunwayregion create <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
        sender.sendMessage("§6/sunwayregion resize <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
        sender.sendMessage("§6/sunwayregion delete <name>");
        sender.sendMessage("§6/sunwayregion decouple <name>");
        sender.sendMessage("§6/sunwayregion trust <name> <player>");
        sender.sendMessage("§6/sunwayregion untrust <name> <player>");
        sender.sendMessage("§6/sunwayregion trustlist <name>");
        sender.sendMessage("§6/sunwayregion list");
        sender.sendMessage("§6/sunwayregion here");
    }

    private boolean createRegion(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§cUsage: /sunwayregion create <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
            return true;
        }

        String name = args[1];
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§cWorld not found: " + worldName);
            return true;
        }

        try {
            int minX = Integer.parseInt(args[3]);
            int minY = Integer.parseInt(args[4]);
            int minZ = Integer.parseInt(args[5]);
            int maxX = Integer.parseInt(args[6]);
            int maxY = Integer.parseInt(args[7]);
            int maxZ = Integer.parseInt(args[8]);

            if (regionManager.createRegion(name, worldName, minX, minY, minZ, maxX, maxY, maxZ, null, false)) {
                sender.sendMessage("§aRegion '" + name + "' created");
            } else {
                sender.sendMessage("§cRegion creation failed (name already exists?)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordinates must be integers");
        }
        return true;
    }

    private boolean resizeRegion(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§cUsage: /sunwayregion resize <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
            return true;
        }

        String name = args[1];
        try {
            int minX = Integer.parseInt(args[2]);
            int minY = Integer.parseInt(args[3]);
            int minZ = Integer.parseInt(args[4]);
            int maxX = Integer.parseInt(args[5]);
            int maxY = Integer.parseInt(args[6]);
            int maxZ = Integer.parseInt(args[7]);

            if (regionManager.updateRegionBounds(name, minX, minY, minZ, maxX, maxY, maxZ)) {
                sender.sendMessage("§aRegion '" + name + "' resized");
            } else {
                sender.sendMessage("§cRegion resize failed (not found or decoupled?)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordinates must be integers");
        }
        return true;
    }

    private boolean deleteRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /sunwayregion delete <name>");
            return true;
        }

        String name = args[1];
        if (regionManager.deleteRegion(name)) {
            sender.sendMessage("§aRegion '" + name + "' deleted");
        } else {
            sender.sendMessage("§cRegion not found: " + name);
        }
        return true;
    }

    private boolean decoupleRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /sunwayregion decouple <name>");
            return true;
        }

        String name = args[1];
        if (regionManager.setDecoupled(name, true)) {
            sender.sendMessage("§aRegion '" + name + "' decoupled from GP");
        } else {
            sender.sendMessage("§cRegion not found: " + name);
        }
        return true;
    }

    private boolean manageTrust(CommandSender sender, String[] args, boolean add) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /sunwayregion " + (add ? "trust" : "untrust") + " <name> <player>");
            return true;
        }

        String name = args[1];
        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            sender.sendMessage("§cPlayer not found: " + args[2]);
            return true;
        }

        if (regionManager.manageTrust(name, player.getUniqueId(), add)) {
            String action = add ? "trusted" : "untrusted";
            sender.sendMessage("§aPlayer " + player.getName() + " " + action + " in region " + name);
        } else {
            sender.sendMessage("§cFailed to modify trust (region not found or not decoupled?)");
        }
        return true;
    }
}
