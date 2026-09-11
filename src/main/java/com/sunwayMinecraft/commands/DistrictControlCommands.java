package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.config.DistrictControlSettingsConfig;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository.ControlHistoryRecord;
import com.sunwayMinecraft.districts.service.DistrictControlService;
import com.sunwayMinecraft.districts.service.DistrictControlService.ControlState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * "/district contest" and "/district control ..." subcommands: player-facing
 * contest status plus admin control management.
 */
public class DistrictControlCommands {
    private static final String ADMIN_PERMISSION = "sunway.district.admin";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final DistrictControlService controlService;
    private final DistrictControlSettingsConfig settings;

    public DistrictControlCommands(DistrictControlService controlService,
                                   DistrictControlSettingsConfig settings) {
        this.controlService = controlService;
        this.settings = settings;
    }

    /** /district contest - contest status in the player's current district. */
    public boolean handleContest(CommandSender sender, Player player) {
        DistrictDefinition district = currentDistrict(player);
        if (district == null) {
            sender.sendMessage("§7You are not inside a district.");
            return true;
        }
        sendStatus(sender, district, false);
        return true;
    }

    /** /district control [history [district] | status <district>] */
    public boolean handleControl(CommandSender sender, org.bukkit.entity.Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("history")) {
            boolean admin = args.length >= 3;
            if (admin && !sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage("§cYou do not have permission to view other districts' history.");
                return true;
            }
            DistrictDefinition district;
            if (admin) {
                district = districtById(args[2]);
                if (district == null) {
                    sender.sendMessage("§cUnknown district: §f" + args[2]);
                    return true;
                }
            } else {
                district = currentDistrict(player);
                if (district == null) {
                    sender.sendMessage("§7You are not inside a district.");
                    return true;
                }
            }
            sendHistory(sender, district.getId());
            return true;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("status")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage("§cYou do not have permission to query other districts.");
                return true;
            }
            DistrictDefinition district = districtById(args[2]);
            if (district == null) {
                sender.sendMessage("§cUnknown district: §f" + args[2]);
                return true;
            }
            sendStatus(sender, district, true);
            return true;
        }
        if (player == null) {
            sender.sendMessage("§cConsole must use /district control status <district>.");
            return true;
        }
        DistrictDefinition district = currentDistrict(player);
        if (district == null) {
            sender.sendMessage("§7You are not inside a district.");
            return true;
        }
        sendStatus(sender, district, false);
        return true;
    }

    /** /district admin contest|control ... admin operations. */
    public boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cYou do not have permission to administer district control.");
            return true;
        }
        // args: ["admin", "contest|control", <action>, <district>, [alignment]]
        String area = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        String action = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        String actor = sender.getName();
        try {
            if ("contest".equals(area)) {
                switch (action) {
                    case "start" -> controlService.adminStartContest(requireDistrict(args, 3), actor);
                    case "stop" -> controlService.adminStopContest(requireDistrict(args, 3), actor);
                    case "reset" -> controlService.adminResetContest(requireDistrict(args, 3), actor);
                    case "reload" -> settings.reload();
                    default -> {
                        sendAdminHelp(sender);
                        return true;
                    }
                }
                sender.sendMessage("§aContest action '" + action + "' applied.");
                return true;
            }
            if ("control".equals(area)) {
                switch (action) {
                    case "set" -> {
                        if (args.length < 5) {
                            sender.sendMessage("§cUsage: /district admin control set <district> <alignment>");
                            return true;
                        }
                        controlService.adminSetControl(requireDistrict(args, 3), args[4].toLowerCase(Locale.ROOT), actor);
                    }
                    case "clear" -> controlService.adminClearControl(requireDistrict(args, 3), actor);
                    case "lock" -> controlService.adminLock(requireDistrict(args, 3), actor);
                    case "unlock" -> controlService.adminUnlock(requireDistrict(args, 3), actor);
                    case "rebuild-overrides" -> controlService.rebuildOverrides(actor);
                    default -> {
                        sendAdminHelp(sender);
                        return true;
                    }
                }
                sender.sendMessage("§aControl action '" + action + "' applied.");
                return true;
            }
            sendAdminHelp(sender);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6/district admin contest <start|stop|reset|reload> <district>");
        sender.sendMessage("§6/district admin control <set|clear|lock|unlock|rebuild-overrides> <district> [alignment]");
    }

    private void sendStatus(CommandSender sender, DistrictDefinition district, boolean adminView) {
        Optional<ControlState> state = controlService.getControlState(district.getId());
        sender.sendMessage("§6=== Control: " + district.getDisplayName() + " ===");
        if (state.isEmpty()) {
            sender.sendMessage("§7State: §fstable §7- no contest recorded.");
            sender.sendMessage("§7Controller: §fNeutral");
            return;
        }
        ControlState control = state.get();
        DistrictControlSettingsConfig controlSettings = settings;
        int required = controlSettings.getPointsRequiredToCapture();
        sender.sendMessage("§eController: §f"
                + (control.controllerAlignmentId == null ? "Neutral" : control.controllerAlignmentId));
        if (control.controllerGrandAllianceId != null) {
            sender.sendMessage("§eGrand Alliance: §f" + control.controllerGrandAllianceId);
        }
        sender.sendMessage("§eState: §f" + control.state.name().toLowerCase(Locale.ROOT));
        if (control.leadingAlignmentId != null) {
            sender.sendMessage("§eLeading: §f" + control.leadingAlignmentId + " §7(§e"
                    + control.getProgressPercent(required) + "%§7)");
        }
        if (control.state == com.sunwayMinecraft.districts.service.DistrictControlService.ContestState.COOLING_DOWN) {
            long seconds = Math.max(0, (control.cooldownUntil - System.currentTimeMillis()) / 1000);
            sender.sendMessage("§eCooldown: §f" + seconds + "s");
        }
    }

    private void sendHistory(CommandSender sender, String districtId) {
        List<ControlHistoryRecord> history = controlService.getRepository().getHistory(districtId, 10);
        sender.sendMessage("§6=== Control History: " + districtId + " ===");
        if (history.isEmpty()) {
            sender.sendMessage("§7No control changes recorded.");
            return;
        }
        for (ControlHistoryRecord record : history) {
            sender.sendMessage("§e" + DATE_FORMAT.format(Instant.ofEpochMilli(record.changedAt()))
                    + " §f" + (record.newAlignmentId() == null ? "Neutral" : record.newAlignmentId())
                    + " §7(" + record.reason() + ")");
        }
    }

    private com.sunwayMinecraft.districts.domain.DistrictDefinition currentDistrict(Player player) {
        return controlService.getDistrictAt(player.getLocation());
    }

    private com.sunwayMinecraft.districts.domain.DistrictDefinition districtById(String id) {
        return controlService.getDistrictById(id);
    }

    private String requireDistrict(String[] args, int index) {
        DistrictDefinition district = districtById(args[index]);
        if (district == null) {
            throw new IllegalArgumentException("Unknown district: " + args[index]);
        }
        return district.getId();
    }
}
