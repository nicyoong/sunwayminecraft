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
}
