package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.util.DistrictFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /districtadmin <list|info|at|validate|reload>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                sender.sendMessage(Component.text("All Districts:", NamedTextColor.GOLD));
                List<DistrictDefinition> all = new ArrayList<>(districtManager.getPublicDistricts());
                all.sort(Comparator.comparing(DistrictDefinition::getId, String.CASE_INSENSITIVE_ORDER));
                for (DistrictDefinition district : all) {
                    sender.sendMessage(
                            Component.text("- ", NamedTextColor.YELLOW)
                                    .append(Component.text(district.getId(), NamedTextColor.WHITE))
                                    .append(Component.text(" (", NamedTextColor.GRAY))
                                    .append(Component.text(DistrictFormatter.compactLine(district), NamedTextColor.GRAY))
                                    .append(Component.text(")", NamedTextColor.GRAY))
                    );
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /districtadmin info <id>", NamedTextColor.RED));
                    return true;
                }
                districtCommands.sendAdminDistrictInfo(sender, args[1]);
            }
            case "at" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use /districtadmin at.", NamedTextColor.RED));
                    return true;
                }
                DistrictDefinition district = districtManager.getDistrictAt(player.getLocation());
                if (district == null) {
                    sender.sendMessage(Component.text("No district found at your current location.", NamedTextColor.YELLOW));
                } else {
                    districtCommands.sendAdminDistrictInfo(sender, district.getId());
                }
            }
            case "validate" -> {
                List<String> errors = districtManager.validate();
                if (errors.isEmpty()) {
                    sender.sendMessage(Component.text("District validation passed.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(
                            Component.text("District validation found " + errors.size() + " issue(s):", NamedTextColor.RED)
                    );
                    for (String error : errors) {
                        sender.sendMessage(
                                Component.text("- ", NamedTextColor.GRAY)
                                        .append(Component.text(error, NamedTextColor.GRAY))
                        );
                    }
                }
            }
            case "reload" -> {
                districtManager.reload();
                sender.sendMessage(Component.text("District configuration reloaded.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }

        return true;
    }
}
