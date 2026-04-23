package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.util.DistrictFormatter;
import org.bukkit.ChatColor;
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
                sender.sendMessage(ChatColor.RED + "Console must use /district list or /district info <id>.");
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
                    sender.sendMessage(ChatColor.RED + "Usage: /district info <id>");
                    return true;
                }
                sendDistrictInfo(sender, args[1], false);
            }
            default -> {
                if (sender instanceof Player player) {
                    sendCurrentDistrict(player);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                }
            }
        }
        return true;
    }

    private void sendCurrentDistrict(Player player) {
        DistrictDefinition district = districtManager.getDistrictAt(player.getLocation());
        if (district == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not in a registered district.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "District: " + ChatColor.YELLOW + district.getDisplayName());
        player.sendMessage(ChatColor.GRAY + DistrictFormatter.prestigeLabel(district.getPrestigeTier())
            + " " + DistrictFormatter.typeLabel(district.getDistrictType()));
        player.sendMessage(ChatColor.GRAY + DistrictFormatter.safeSummary(district));
        if (!district.getTags().isEmpty()) {
            player.sendMessage(ChatColor.DARK_GRAY + "Tags: " + ChatColor.GRAY + DistrictFormatter.formatTags(district));
        }
    }

    private void sendPublicDistrictList(CommandSender sender) {
        List<DistrictDefinition> districts = districtManager.getPublicDistricts();
        if (districts.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No public districts are currently listed.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Public Districts:");
        for (DistrictDefinition district : districts) {
            sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + DistrictFormatter.compactLine(district));
        }
    }

    private void sendDistrictInfo(CommandSender sender, String id, boolean adminView) {
        DistrictDefinition district = districtManager.getDistrict(id);
        if (district == null || (!adminView && !district.isPublicVisible())) {
            sender.sendMessage(ChatColor.RED + "District not found.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "District: " + ChatColor.YELLOW + district.getDisplayName());
        if (district.getShortName() != null) {
            sender.sendMessage(ChatColor.GRAY + "Short Name: " + ChatColor.WHITE + district.getShortName());
        }
        sender.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.WHITE + DistrictFormatter.typeLabel(district.getDistrictType()));
        sender.sendMessage(ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + DistrictFormatter.prestigeLabel(district.getPrestigeTier()));
        sender.sendMessage(ChatColor.GRAY + "Summary: " + ChatColor.WHITE + DistrictFormatter.safeSummary(district));
        sender.sendMessage(ChatColor.GRAY + "Tags: " + ChatColor.WHITE + DistrictFormatter.formatTags(district));

        if (adminView) {
            sender.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + district.getId());
            sender.sendMessage(ChatColor.DARK_GRAY + "World: " + ChatColor.GRAY + district.getWorld());
            sender.sendMessage(ChatColor.DARK_GRAY + "Public Visible: " + ChatColor.GRAY + district.isPublicVisible());
            sender.sendMessage(ChatColor.DARK_GRAY + "Listing Priority: " + ChatColor.GRAY + district.getListingPriority());
            sender.sendMessage(ChatColor.DARK_GRAY + "Storefront Priority: " + ChatColor.GRAY + district.isStorefrontPriority());
            sender.sendMessage(ChatColor.DARK_GRAY + "Residency Priority: " + ChatColor.GRAY + district.isResidencyPriority());
            sender.sendMessage(ChatColor.DARK_GRAY + "Approval Bias: " + ChatColor.GRAY + district.getRecommendedApprovalBias());
            sender.sendMessage(ChatColor.DARK_GRAY + "Allow Public Events: " + ChatColor.GRAY + district.isAllowPublicEvents());
            sender.sendMessage(ChatColor.DARK_GRAY + "Signature Area: " + ChatColor.GRAY + district.isSignatureArea());
            sender.sendMessage(ChatColor.DARK_GRAY + "Region: " + ChatColor.GRAY + district.getRegion());
        }
    }

    public void sendAdminDistrictInfo(CommandSender sender, String id) {
        sendDistrictInfo(sender, id, true);
    }
}
