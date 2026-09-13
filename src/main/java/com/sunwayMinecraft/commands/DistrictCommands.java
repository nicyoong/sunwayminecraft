package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
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

    private static final int LIST_PAGE_SIZE = 8;

    private final DistrictManager districtManager;
    private final DistrictAlignmentService alignmentService;
    private final DistrictAdminSubCommands adminSubCommands;
    private final DistrictControlCommands controlCommands;

    public DistrictCommands(DistrictManager districtManager, DistrictAlignmentService alignmentService,
                            DistrictAdminSubCommands adminSubCommands, DistrictControlCommands controlCommands) {
        this.districtManager = districtManager;
        this.alignmentService = alignmentService;
        this.adminSubCommands = adminSubCommands;
        this.controlCommands = controlCommands;
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
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "admin":
                return adminSubCommands.handle(sender, args);
            case "contest":
                return controlCommands.handleContest(sender,
                        sender instanceof Player p ? p : null);
            case "control":
                return controlCommands.handleControl(sender, sender instanceof Player p ? p : null, args);
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

    private boolean handleList(CommandSender sender, String[] args) {
        List<DistrictDefinition> districts = districtManager.getPublicDistricts();

        if (districts.isEmpty()) {
            sender.sendMessage(Component.text("There are no public districts available.", NamedTextColor.GRAY));
            return true;
        }

        // group by home campus when there is one, otherwise by district type;
        // "list type" forces type grouping
        boolean byCampus = !args[0].equalsIgnoreCase("list") || args.length < 2
                || !args[1].equalsIgnoreCase("type");

        int totalPages = Math.max(1, (districts.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE);
        int page = parsePage(args.length >= 3 ? args[2] : "1", totalPages);
        sender.sendMessage(Component.text("Public Districts (page " + page + "/" + totalPages + "):",
                NamedTextColor.GOLD));

        String lastGroup = null;
        int start = (page - 1) * LIST_PAGE_SIZE;
        for (int i = start; i < Math.min(start + LIST_PAGE_SIZE, districts.size()); i++) {
            DistrictDefinition district = districts.get(i);
            String group = byCampus
                    ? java.util.Optional.ofNullable(district.getOwnership().homeCampus())
                        .orElseGet(() -> DistrictFormatter.formatDistrictType(district.getDistrictType()))
                    : DistrictFormatter.formatDistrictType(district.getDistrictType());
            if (!group.equals(lastGroup)) {
                lastGroup = group;
                sender.sendMessage(Component.text(group + ":", NamedTextColor.DARK_AQUA));
            }
            sender.sendMessage(
                    Component.text("- ", NamedTextColor.YELLOW)
                            .append(Component.text(district.getDisplayName(), NamedTextColor.YELLOW))
                            .append(Component.text(" (", NamedTextColor.GRAY))
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

    private int parsePage(String raw, int totalPages) {
        try {
            return Math.min(Math.max(1, Integer.parseInt(raw)), totalPages);
        } catch (NumberFormatException e) {
            return 1;
        }
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

        appendAllianceInfo(sender, district);

        if (!district.getTags().isEmpty()) {
            sender.sendMessage(
                    Component.text("Tags: ", NamedTextColor.GRAY)
                            .append(Component.text(String.join(", ", district.getTags()), NamedTextColor.WHITE))
            );
        }
    }

    /** Campus, ownership, transit, contest and personal access lines. */
    private void appendAllianceInfo(CommandSender sender, DistrictDefinition district) {
        com.sunwayMinecraft.districts.domain.DistrictOwnership ownership = district.getOwnership();

        if (ownership.homeCampus() != null) {
            sender.sendMessage(Component.text("Campus: ", NamedTextColor.GRAY)
                    .append(Component.text(ownership.homeCampus(), NamedTextColor.WHITE)));
        }
        String owner = ownership.alignmentOwner() != null
                ? ownership.alignmentOwner()
                : ownership.grandAllianceOwner();
        sender.sendMessage(Component.text("Owner: ", NamedTextColor.GRAY)
                .append(Component.text(owner != null ? owner : "Neutral", NamedTextColor.WHITE)));
        if (ownership.contested()) {
            sender.sendMessage(Component.text("Status: ", NamedTextColor.GRAY)
                    .append(Component.text("CONTESTED", NamedTextColor.RED)));
        }
        if (ownership.transitConnected()) {
            sender.sendMessage(Component.text("Transit: ", NamedTextColor.GRAY)
                    .append(Component.text("Connected", NamedTextColor.AQUA)));
        }
        if (sender instanceof Player viewer) {
            boolean allowed = alignmentService.canPlayerAccessDistrict(viewer, district);
            sender.sendMessage(Component.text("Access: ", NamedTextColor.GRAY)
                    .append(Component.text(allowed ? "Allowed" : "Denied",
                            allowed ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sunway.district.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(args[0], List.of("list", "info", "admin", "contest", "control"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return filterPrefix(args[1], List.of("campus", "type"));
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
