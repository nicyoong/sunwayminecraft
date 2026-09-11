package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "/district admin ..." subcommands: runtime ownership, type and access
 * mutations plus override persistence. Every action requires
 * sunway.district.admin and is logged by the config manager.
 */
public class DistrictAdminSubCommands {
    private final DistrictManager districtManager;
    private final DistrictsConfigManager configManager;

    public DistrictAdminSubCommands(DistrictManager districtManager, DistrictsConfigManager configManager) {
        this.districtManager = districtManager;
        this.configManager = configManager;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sunway.district.admin")) {
            sender.sendMessage("§cYou do not have permission to administer districts.");
            return true;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "help";
        try {
            switch (sub) {
                case "reload" -> {
                    districtManager.reload();
                    sender.sendMessage("§aDistrict configuration reloaded (overrides reapplied).");
                }
                case "save" -> {
                    configManager.saveOverrides();
                    sender.sendMessage("§aDistrict overrides saved to district-overrides.yml.");
                }
                case "set-owner" -> handleSetOwner(sender, args);
                case "clear-owner" -> handleClearOwner(sender, args);
                case "set-type" -> handleSetType(sender, args);
                case "toggle" -> handleToggle(sender, args);
                case "add-allowed", "remove-allowed" -> handleListChange(sender, args, true);
                case "add-denied", "remove-denied" -> handleListChange(sender, args, false);
                default -> sendHelp(sender);
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
        return true;
    }

    private void handleSetOwner(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /district admin set-owner <district> <alignment>");
            return;
        }
        DistrictDefinition district = requireDistrict(args[2]);
        String alignment = args[3].toLowerCase(Locale.ROOT);
        String alliance = configManager.allianceOfAlignment(alignment);
        if (alliance == null) {
            sender.sendMessage("§cUnknown alignment: §f" + alignment
                    + "§c (alignments.yml must define it).");
            return;
        }
        DistrictOwnership current = district.getOwnership();
        configManager.updateOwnership(district.getId(), new DistrictOwnership(
                current.homeCampus(), alliance, alignment,
                current.allowedAlignments(), current.deniedAlignments(),
                current.propertyPolicy(), current.transitConnected(), current.contested()),
                sender.getName());
        sender.sendMessage("§a" + district.getDisplayName() + " §7is now owned by §f" + alignment
                + " §7(" + alliance + ").");
    }

    private void handleClearOwner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /district admin clear-owner <district>");
            return;
        }
        DistrictDefinition district = requireDistrict(args[2]);
        DistrictOwnership current = district.getOwnership();
        configManager.updateOwnership(district.getId(), new DistrictOwnership(
                current.homeCampus(), null, null,
                List.of(), List.of(),
                current.propertyPolicy(), current.transitConnected(), current.contested()),
                sender.getName());
        sender.sendMessage("§a" + district.getDisplayName() + " §7is now neutral.");
    }

    private void handleSetType(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /district admin set-type <district> <type>");
            return;
        }
        DistrictDefinition district = requireDistrict(args[2]);
        DistrictType type;
        try {
            type = DistrictType.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUnknown district type: §f" + args[3]);
            return;
        }
        configManager.updateDistrictType(district.getId(), type, sender.getName());
        sender.sendMessage("§a" + district.getDisplayName() + " §7is now a §f" + type + "§7 district.");
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /district admin toggle <contested|transit> <district>");
            return;
        }
        String target = args[2].toLowerCase(Locale.ROOT);
        DistrictDefinition district = requireDistrict(args[3]);
        DistrictOwnership current = district.getOwnership();
        switch (target) {
            case "contested" -> {
                boolean newValue = !current.contested();
                configManager.updateOwnership(district.getId(), new DistrictOwnership(
                        current.homeCampus(), current.grandAllianceOwner(), current.alignmentOwner(),
                        current.allowedAlignments(), current.deniedAlignments(),
                        current.propertyPolicy(), current.transitConnected(), newValue),
                        sender.getName());
                sender.sendMessage("§a" + district.getDisplayName() + " contested: §f" + newValue);
            }
            case "transit" -> {
                boolean newValue = !current.transitConnected();
                configManager.updateOwnership(district.getId(), new DistrictOwnership(
                        current.homeCampus(), current.grandAllianceOwner(), current.alignmentOwner(),
                        current.allowedAlignments(), current.deniedAlignments(),
                        current.propertyPolicy(), newValue, current.contested()),
                        sender.getName());
                sender.sendMessage("§a" + district.getDisplayName() + " transit: §f" + newValue);
            }
            default -> sender.sendMessage("§cToggle what? contested or transit.");
        }
    }

    private void handleListChange(CommandSender sender, String[] args, boolean allowed) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /district admin " + args[1] + " <district> <alignment>");
            return;
        }
        DistrictDefinition district = requireDistrict(args[2]);
        String alignment = args[3].toLowerCase(Locale.ROOT);
        DistrictOwnership current = district.getOwnership();
        List<String> list = new ArrayList<>(allowed ? current.allowedAlignments() : current.deniedAlignments());
        boolean adding = args[1].toLowerCase(Locale.ROOT).startsWith("add");
        if (adding) {
            if (list.contains(alignment)) {
                sender.sendMessage("§7" + alignment + " is already on the " + (allowed ? "allowed" : "denied")
                        + " list of this district.");
                return;
            }
            list.add(alignment);
        } else if (!list.remove(alignment)) {
            sender.sendMessage("§7" + alignment + " is not on the " + (allowed ? "allowed" : "denied")
                    + " list of this district.");
            return;
        }
        configManager.updateOwnership(district.getId(), new DistrictOwnership(
                current.homeCampus(), current.grandAllianceOwner(), current.alignmentOwner(),
                allowed ? list : current.allowedAlignments(),
                !allowed ? list : current.deniedAlignments(),
                current.propertyPolicy(), current.transitConnected(), current.contested()),
                sender.getName());
        sender.sendMessage("§aUpdated the " + (allowed ? "allowed" : "denied") + " list of §f"
                + district.getDisplayName() + "§a.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6/district admin <reload|save|set-owner|clear-owner|set-type|"
                + "toggle|add-allowed|remove-allowed|add-denied|remove-denied>");
    }

    private DistrictDefinition requireDistrict(String id) {
        DistrictDefinition district = districtManager.getDistrict(id);
        if (district == null) {
            throw new IllegalArgumentException("Unknown district: " + id);
        }
        return district;
    }
}
