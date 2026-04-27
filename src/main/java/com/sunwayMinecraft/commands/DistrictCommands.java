package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.util.DistrictFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DistrictCommands implements CommandExecutor, TabCompleter {

    private final DistrictManager districtManager;

    public DistrictCommands(DistrictManager districtManager) {
        this.districtManager = districtManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.district.use")) {
            sender.sendMessage(Component.text("You do not have permission to use district commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return handleCurrentDistrict(sender);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                sender.sendMessage(
                        Component.text("Usage: ", NamedTextColor.GRAY)
                                .append(Component.text("/district [list|info <districtId>]", NamedTextColor.YELLOW))
                );
                return true;
        }
    }

    private boolean handleCurrentDistrict(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /district without arguments.", NamedTextColor.RED));
            sender.sendMessage(
                    Component.text("Use ", NamedTextColor.GRAY)
                            .append(Component.text("/district list", NamedTextColor.YELLOW))
                            .append(Component.text(" or ", NamedTextColor.GRAY))
                            .append(Component.text("/district info <districtId>", NamedTextColor.YELLOW))
                            .append(Component.text(" instead.", NamedTextColor.GRAY))
            );
            return true;
        }

        Location location = player.getLocation();
        DistrictDefinition district = districtManager.getDistrictAt(location);

        if (district == null || !district.isPublicVisible()) {
            player.sendMessage(Component.text("You are not currently inside a public district.", NamedTextColor.GRAY));
            return true;
        }

        sendPublicDistrictInfo(player, district);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getPublicDistricts();

        if (districts.isEmpty()) {
            sender.sendMessage(Component.text("There are no public districts available.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("Public Districts:", NamedTextColor.GOLD));

        for (DistrictDefinition district : districts) {
            sender.sendMessage(
                    Component.text("- ", NamedTextColor.YELLOW)
                            .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(DistrictFormatter.formatDistrictType(district.getDistrictType()), NamedTextColor.WHITE))
                            .append(Component.text(", ", NamedTextColor.GRAY))
                            .append(Component.text(DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
                            .append(Component.text(")", NamedTextColor.GRAY))
            );

            if (!district.getTags().isEmpty()) {
                sender.sendMessage(
                        Component.text("  Tags: ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(String.join(", ", district.getTags()), NamedTextColor.GRAY))
                );
            }
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.text("Usage: ", NamedTextColor.RED)
                            .append(Component.text("/district info <districtId>", NamedTextColor.YELLOW))
            );
            return true;
        }

        DistrictDefinition district = districtManager.getDistrict(args[1]);
        if (district == null || !district.isPublicVisible()) {
            sender.sendMessage(Component.text("District not found.", NamedTextColor.RED));
            return true;
        }

        sendPublicDistrictInfo(sender, district);
        return true;
    }

    private void sendPublicDistrictInfo(CommandSender sender, DistrictDefinition district) {
        sender.sendMessage(
                Component.text("District: ", NamedTextColor.GOLD)
                        .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
        );

        if (district.getShortName() != null && !district.getShortName().isBlank()) {
            sender.sendMessage(
                    Component.text("Short Name: ", NamedTextColor.GRAY)
                            .append(Component.text(district.getShortName(), NamedTextColor.WHITE))
            );
        }

        sender.sendMessage(
                Component.text("Type: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatDistrictType(district.getDistrictType()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Prestige: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Summary: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getPublicSummary(), NamedTextColor.WHITE))
        );

        if (!district.getTags().isEmpty()) {
            sender.sendMessage(
                    Component.text("Tags: ", NamedTextColor.GRAY)
                            .append(Component.text(String.join(", ", district.getTags()), NamedTextColor.WHITE))
            );
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sunway.district.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(args[0], List.of("list", "info"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> districtIds = new ArrayList<>();
            for (DistrictDefinition district : districtManager.getPublicDistricts()) {
                districtIds.add(district.getId());
            }
            return filterPrefix(args[1], districtIds);
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
