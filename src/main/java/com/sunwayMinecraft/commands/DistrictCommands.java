package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.util.DistrictFormatter;
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
            sender.sendMessage("§cYou do not have permission to use district commands.");
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
                sender.sendMessage("§cUnknown subcommand.");
                sender.sendMessage("§7Usage: /district [list|info <districtId>]");
                return true;
        }
    }

    private boolean handleCurrentDistrict(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use /district without arguments.");
            sender.sendMessage("§7Use /district list or /district info <districtId> instead.");
            return true;
        }

        Location location = player.getLocation();
        DistrictDefinition district = districtManager.getDistrictAt(location);

        if (district == null || !district.isPublicVisible()) {
            player.sendMessage("§7You are not currently inside a public district.");
            return true;
        }

        sendPublicDistrictInfo(player, district);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getPublicDistricts();

        if (districts.isEmpty()) {
            sender.sendMessage("§7There are no public districts available.");
            return true;
        }

        sender.sendMessage("§6Public Districts:");
        for (DistrictDefinition district : districts) {
            String line = "§e- " + district.getDisplayName()
                    + " §7(" + DistrictFormatter.formatDistrictType(district.getDistrictType())
                    + ", " + DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()) + "§7)";
            sender.sendMessage(line);

            if (!district.getTags().isEmpty()) {
                sender.sendMessage("  §8Tags: §7" + String.join(", ", district.getTags()));
            }
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /district info <districtId>");
            return true;
        }

        DistrictDefinition district = districtManager.getDistrict(args[1]);
        if (district == null || !district.isPublicVisible()) {
            sender.sendMessage("§cDistrict not found.");
            return true;
        }

        sendPublicDistrictInfo(sender, district);
        return true;
    }

    private void sendPublicDistrictInfo(CommandSender sender, DistrictDefinition district) {
        sender.sendMessage("§6District: §e" + district.getDisplayName());

        if (district.getShortName() != null && !district.getShortName().isBlank()) {
            sender.sendMessage("§7Short Name: §f" + district.getShortName());
        }

        sender.sendMessage("§7Type: §f" + DistrictFormatter.formatDistrictType(district.getDistrictType()));
        sender.sendMessage("§7Prestige: §f" + DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()));
        sender.sendMessage("§7Summary: §f" + district.getPublicSummary());

        if (!district.getTags().isEmpty()) {
            sender.sendMessage("§7Tags: §f" + String.join(", ", district.getTags()));
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
