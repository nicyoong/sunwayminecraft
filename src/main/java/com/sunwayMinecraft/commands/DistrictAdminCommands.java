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

public class DistrictAdminCommands implements CommandExecutor, TabCompleter {

    private final DistrictManager districtManager;

    public DistrictAdminCommands(DistrictManager districtManager) {
        this.districtManager = districtManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.district.admin")) {
            sender.sendMessage("§cYou do not have permission to use district admin commands.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list":
                return handleList(sender);

            case "info":
                return handleInfo(sender, args);

            case "at":
                return handleAt(sender);

            case "validate":
                return handleValidate(sender);

            case "reload":
                return handleReload(sender);

            default:
                sender.sendMessage("§cUnknown subcommand.");
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6District Admin Commands:");
        sender.sendMessage("§e/districtadmin list");
        sender.sendMessage("§e/districtadmin info <districtId>");
        sender.sendMessage("§e/districtadmin at");
        sender.sendMessage("§e/districtadmin validate");
        sender.sendMessage("§e/districtadmin reload");
    }

    private boolean handleList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getAllDistricts();

        if (districts.isEmpty()) {
            sender.sendMessage("§7No districts are loaded.");
            return true;
        }

        sender.sendMessage("§6All Districts:");
        for (DistrictDefinition district : districts) {
            String line = "§e- " + district.getId()
                    + " §7-> §f" + district.getDisplayName()
                    + " §7(" + DistrictFormatter.formatDistrictType(district.getDistrictType())
                    + ", " + DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()) + "§7)";
            sender.sendMessage(line);
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /districtadmin info <districtId>");
            return true;
        }

        DistrictDefinition district = districtManager.getDistrict(args[1]);
        if (district == null) {
            sender.sendMessage("§cDistrict not found.");
            return true;
        }

        sender.sendMessage("§6District Admin Info: §e" + district.getDisplayName());
        sender.sendMessage("§7ID: §f" + district.getId());

        if (district.getShortName() != null && !district.getShortName().isBlank()) {
            sender.sendMessage("§7Short Name: §f" + district.getShortName());
        }

        sender.sendMessage("§7World: §f" + district.getWorldName());
        sender.sendMessage("§7Enabled: §f" + district.isEnabled());
        sender.sendMessage("§7Public Visible: §f" + district.isPublicVisible());
        sender.sendMessage("§7Type: §f" + DistrictFormatter.formatDistrictType(district.getDistrictType()));
        sender.sendMessage("§7Prestige Tier: §f" + district.getPrestigeTier()
                + " §7(" + DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()) + "§7)");
        sender.sendMessage("§7Public Summary: §f" + district.getPublicSummary());

        if (!district.getTags().isEmpty()) {
            sender.sendMessage("§7Tags: §f" + String.join(", ", district.getTags()));
        }

        sender.sendMessage("§7Listing Priority: §f" + district.getListingPriority());
        sender.sendMessage("§7Storefront Priority: §f" + district.isStorefrontPriority());
        sender.sendMessage("§7Residency Priority: §f" + district.isResidencyPriority());
        sender.sendMessage("§7Approval Bias: §f" + district.getRecommendedApprovalBias().name());
        sender.sendMessage("§7Allow Public Events: §f" + district.isAllowPublicEvents());
        sender.sendMessage("§7Signature Area: §f" + district.isSignatureArea());
        sender.sendMessage("§7Region: §f" + formatRegion(district));

        return true;
    }

    private boolean handleAt(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use /districtadmin at.");
            return true;
        }

        Location location = player.getLocation();
        DistrictDefinition district = districtManager.getDistrictAt(location);

        if (district == null) {
            sender.sendMessage("§7No district found at your current location.");
            return true;
        }

        sender.sendMessage("§6Current District:");
        sender.sendMessage("§7ID: §f" + district.getId());
        sender.sendMessage("§7Name: §f" + district.getDisplayName());
        sender.sendMessage("§7Type: §f" + DistrictFormatter.formatDistrictType(district.getDistrictType()));
        sender.sendMessage("§7Prestige: §f" + DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()));
        sender.sendMessage("§7Region: §f" + formatRegion(district));

        return true;
    }

    private boolean handleValidate(CommandSender sender) {
        List<String> errors = districtManager.validateDistricts();

        if (errors.isEmpty()) {
            sender.sendMessage("§aDistrict validation passed with no errors.");
            return true;
        }

        sender.sendMessage("§cDistrict validation found " + errors.size() + " error(s):");
        for (String error : errors) {
            sender.sendMessage("§7- §c" + error);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        districtManager.reload();
        sender.sendMessage("§aDistricts configuration reloaded.");
        return true;
    }

    private String formatRegion(DistrictDefinition district) {
        return "("
                + district.getRegion().getMinX() + ", "
                + district.getRegion().getMinY() + ", "
                + district.getRegion().getMinZ() + ") -> ("
                + district.getRegion().getMaxX() + ", "
                + district.getRegion().getMaxY() + ", "
                + district.getRegion().getMaxZ() + ")";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sunway.district.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(args[0], List.of("list", "info", "at", "validate", "reload"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> districtIds = new ArrayList<>();
            for (DistrictDefinition district : districtManager.getAllDistricts()) {
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
