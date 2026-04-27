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

public class DistrictAdminCommands implements CommandExecutor, TabCompleter {

    private final DistrictManager districtManager;

    public DistrictAdminCommands(DistrictManager districtManager) {
        this.districtManager = districtManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.district.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use district admin commands.", NamedTextColor.RED));
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
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("District Admin Commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/districtadmin list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/districtadmin info <districtId>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/districtadmin at", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/districtadmin validate", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/districtadmin reload", NamedTextColor.YELLOW));
    }

    private boolean handleList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getAllDistricts();

        if (districts.isEmpty()) {
            sender.sendMessage(Component.text("No districts are loaded.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("All Districts:", NamedTextColor.GOLD));

        for (DistrictDefinition district : districts) {
            sender.sendMessage(
                    Component.text("- ", NamedTextColor.YELLOW)
                            .append(Component.text(district.getId(), NamedTextColor.YELLOW))
                            .append(Component.text(" -> ", NamedTextColor.GRAY))
                            .append(Component.text(district.getDisplayName(), NamedTextColor.WHITE))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(DistrictFormatter.formatDistrictType(district.getDistrictType()), NamedTextColor.WHITE))
                            .append(Component.text(", ", NamedTextColor.GRAY))
                            .append(Component.text(DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
                            .append(Component.text(")", NamedTextColor.GRAY))
            );
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.text("Usage: ", NamedTextColor.RED)
                            .append(Component.text("/districtadmin info <districtId>", NamedTextColor.YELLOW))
            );
            return true;
        }

        DistrictDefinition district = districtManager.getDistrict(args[1]);
        if (district == null) {
            sender.sendMessage(Component.text("District not found.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(
                Component.text("District Admin Info: ", NamedTextColor.GOLD)
                        .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
        );

        sender.sendMessage(
                Component.text("ID: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getId(), NamedTextColor.WHITE))
        );

        if (district.getShortName() != null && !district.getShortName().isBlank()) {
            sender.sendMessage(
                    Component.text("Short Name: ", NamedTextColor.GRAY)
                            .append(Component.text(district.getShortName(), NamedTextColor.WHITE))
            );
        }

        sender.sendMessage(
                Component.text("World: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getWorldName(), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Enabled: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isEnabled()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Public Visible: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isPublicVisible()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Type: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatDistrictType(district.getDistrictType()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Prestige Tier: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.getPrestigeTier()), NamedTextColor.WHITE))
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.GRAY))
        );

        sender.sendMessage(
                Component.text("Public Summary: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getPublicSummary(), NamedTextColor.WHITE))
        );

        if (!district.getTags().isEmpty()) {
            sender.sendMessage(
                    Component.text("Tags: ", NamedTextColor.GRAY)
                            .append(Component.text(String.join(", ", district.getTags()), NamedTextColor.WHITE))
            );
        }

        sender.sendMessage(
                Component.text("Listing Priority: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.getListingPriority()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Storefront Priority: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isStorefrontPriority()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Residency Priority: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isResidencyPriority()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Approval Bias: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getRecommendedApprovalBias().name(), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Allow Public Events: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isAllowPublicEvents()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Signature Area: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(district.isSignatureArea()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Region: ", NamedTextColor.GRAY)
                        .append(Component.text(formatRegion(district), NamedTextColor.WHITE))
        );

        return true;
    }

    private boolean handleAt(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /districtadmin at.", NamedTextColor.RED));
            return true;
        }

        Location location = player.getLocation();
        DistrictDefinition district = districtManager.getDistrictAt(location);

        if (district == null) {
            sender.sendMessage(Component.text("No district found at your current location.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("Current District:", NamedTextColor.GOLD));

        sender.sendMessage(
                Component.text("ID: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getId(), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Name: ", NamedTextColor.GRAY)
                        .append(Component.text(district.getDisplayName(), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Type: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatDistrictType(district.getDistrictType()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Prestige: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatPrestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Region: ", NamedTextColor.GRAY)
                        .append(Component.text(formatRegion(district), NamedTextColor.WHITE))
        );

        return true;
    }

    private boolean handleValidate(CommandSender sender) {
        List<String> errors = districtManager.validateDistricts();

        if (errors.isEmpty()) {
            sender.sendMessage(Component.text("District validation passed with no errors.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(
                Component.text("District validation found ", NamedTextColor.RED)
                        .append(Component.text(String.valueOf(errors.size()), NamedTextColor.WHITE))
                        .append(Component.text(" error(s):", NamedTextColor.RED))
        );

        for (String error : errors) {
            sender.sendMessage(
                    Component.text("- ", NamedTextColor.GRAY)
                            .append(Component.text(error, NamedTextColor.RED))
            );
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        districtManager.reload();
        sender.sendMessage(Component.text("Districts configuration reloaded.", NamedTextColor.GREEN));
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
