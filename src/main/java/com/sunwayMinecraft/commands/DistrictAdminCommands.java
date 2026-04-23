package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.util.DistrictFormatter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DistrictAdminCommands implements CommandExecutor {
    private final DistrictManager districtManager;
    private final DistrictCommands districtCommands;

    public DistrictAdminCommands(DistrictManager districtManager) {
        this.districtManager = districtManager;
        this.districtCommands = new DistrictCommands(districtManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.districts.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /districtadmin <list|info|at|validate|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "All Districts:");
                List<DistrictDefinition> all = new ArrayList<>(districtManager.getPublicDistricts());
                all.sort(Comparator.comparing(DistrictDefinition::getId, String.CASE_INSENSITIVE_ORDER));
                for (DistrictDefinition district : all) {
                    sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + district.getId()
                        + ChatColor.GRAY + " (" + DistrictFormatter.compactLine(district) + ")");
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /districtadmin info <id>");
                    return true;
                }
                districtCommands.sendAdminDistrictInfo(sender, args[1]);
            }
            case "at" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use /districtadmin at.");
                    return true;
                }
                DistrictDefinition district = districtManager.getDistrictAt(player.getLocation());
                if (district == null) {
                    sender.sendMessage(ChatColor.YELLOW + "No district found at your current location.");
                } else {
                    districtCommands.sendAdminDistrictInfo(sender, district.getId());
                }
            }
            case "validate" -> {
                List<String> errors = districtManager.validate();
                if (errors.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "District validation passed.");
                } else {
                    sender.sendMessage(ChatColor.RED + "District validation found " + errors.size() + " issue(s):");
                    for (String error : errors) {
                        sender.sendMessage(ChatColor.GRAY + "- " + error);
                    }
                }
            }
            case "reload" -> {
                districtManager.reload();
                sender.sendMessage(ChatColor.GREEN + "District configuration reloaded.");
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }

        return true;
    }
}
