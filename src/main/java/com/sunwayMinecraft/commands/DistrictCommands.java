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

import java.util.List;

public class DistrictCommands implements CommandExecutor {
    private final DistrictManager districtManager;

    public DistrictCommands(DistrictManager districtManager) {
        this.districtManager = districtManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must use /district list or /district info <id>.", NamedTextColor.RED));
                return true;
            }
            sendCurrentDistrict(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> sendPublicDistrictList(sender);
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /district info <id>", NamedTextColor.RED));
                    return true;
                }
                sendDistrictInfo(sender, args[1], false);
            }
            default -> {
                if (sender instanceof Player player) {
                    sendCurrentDistrict(player);
                } else {
                    sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                }
            }
        }
        return true;
    }

    private void sendCurrentDistrict(Player player) {
        DistrictDefinition district = districtManager.getDistrictAt(player.getLocation());
        if (district == null) {
            player.sendMessage(Component.text("You are not in a registered district.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(
                Component.text("District: ", NamedTextColor.GOLD)
                        .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
        );

        player.sendMessage(
                Component.text(
                        DistrictFormatter.prestigeLabel(district.getPrestigeTier())
                                + " " + DistrictFormatter.typeLabel(district.getDistrictType()),
                        NamedTextColor.GRAY
                )
        );

        player.sendMessage(
                Component.text(DistrictFormatter.safeSummary(district), NamedTextColor.GRAY)
        );

        if (!district.getTags().isEmpty()) {
            player.sendMessage(
                    Component.text("Tags: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(DistrictFormatter.formatTags(district), NamedTextColor.GRAY))
            );
        }
    }

    private void sendPublicDistrictList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getPublicDistricts();
        if (districts.isEmpty()) {
            sender.sendMessage(Component.text("No public districts are currently listed.", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("Public Districts:", NamedTextColor.GOLD));
        for (DistrictDefinition district : districts) {
            sender.sendMessage(
                    Component.text("- ", NamedTextColor.YELLOW)
                            .append(Component.text(DistrictFormatter.compactLine(district), NamedTextColor.WHITE))
            );
        }
    }

    private void sendDistrictInfo(CommandSender sender, String id, boolean adminView) {
        DistrictDefinition district = districtManager.getDistrict(id);
        if (district == null || (!adminView && !district.isPublicVisible())) {
            sender.sendMessage(Component.text("District not found.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(
                Component.text("District: ", NamedTextColor.GOLD)
                        .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
        );

        if (district.getShortName() != null) {
            sender.sendMessage(
                    Component.text("Short Name: ", NamedTextColor.GRAY)
                            .append(Component.text(district.getShortName(), NamedTextColor.WHITE))
            );
        }

        sender.sendMessage(
                Component.text("Type: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.typeLabel(district.getDistrictType()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Prestige: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.prestigeLabel(district.getPrestigeTier()), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Summary: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.safeSummary(district), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Tags: ", NamedTextColor.GRAY)
                        .append(Component.text(DistrictFormatter.formatTags(district), NamedTextColor.WHITE))
        );

        if (adminView) {
            sender.sendMessage(
                    Component.text("ID: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(district.getId(), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("World: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(district.getWorld(), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Public Visible: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.isPublicVisible()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Listing Priority: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.getListingPriority()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Storefront Priority: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.isStorefrontPriority()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Residency Priority: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.isResidencyPriority()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Approval Bias: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.getRecommendedApprovalBias()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Allow Public Events: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.isAllowPublicEvents()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Signature Area: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.isSignatureArea()), NamedTextColor.GRAY))
            );
            sender.sendMessage(
                    Component.text("Region: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(String.valueOf(district.getRegion()), NamedTextColor.GRAY))
            );
        }
    }

    public void sendAdminDistrictInfo(CommandSender sender, String id) {
        sendDistrictInfo(sender, id, true);
    }
}
