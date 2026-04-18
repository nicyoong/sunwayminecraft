package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import com.sunwayMinecraft.residency.admin.SelectionSession;
import com.sunwayMinecraft.residency.domain.*;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ResidencyAdminCommands implements TabExecutor {
    private final ResidencyManager manager;
    private final AdminSelectionManager selectionManager;

    public ResidencyAdminCommands(ResidencyManager manager, AdminSelectionManager selectionManager) {
        this.manager = manager;
        this.selectionManager = selectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.residency.admin")) {
            sender.sendMessage(Message.error("No permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.initialize();
                sender.sendMessage(Message.ok("Residency configs reloaded."));
                return true;
            }

            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Message.error("Usage: /resadmin info <unitId>"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    sender.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
                sender.sendMessage(Message.info(unit.getDisplayName() + " [" + unit.getId() + "]"));
                sender.sendMessage(" District: " + unit.getDistrictId() + ", Building: " + unit.getBuildingId());
                sender.sendMessage(" Lease state: " + record.getLeaseState() + ", Rent state: " + record.getRentState());
                sender.sendMessage(" Tenant: " + (record.getTenantPlayerId() == null ? "none" : record.getTenantPlayerId()));
                return true;
            }

            case "assign" -> {
                if (args.length < 3) {
                    sender.sendMessage(Message.error("Usage: /resadmin assign <unitId> <player>"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    sender.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                boolean ok = manager.getBillingService().startLease(unit, target, unit.getListingSettings().isApprovalRequired());
                sender.sendMessage(ok ? Message.ok("Lease assigned.") : Message.error("Could not assign lease."));
                return true;
            }

            case "terminate" -> {
                if (args.length < 2) {
                    sender.sendMessage(Message.error("Usage: /resadmin terminate <unitId>"));
                    return true;
                }

                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.setLeaseState(LeaseState.REPOSSESSED);
                rec.setLeaseEnd(Instant.now());
                rec.setTenantPlayerId(null);
                rec.getManagerIds().clear();
                manager.getRepository().saveTenancy(rec);
                sender.sendMessage(Message.ok("Lease terminated."));
                return true;
            }

            case "repossess" -> {
                if (args.length < 2) {
                    sender.sendMessage(Message.error("Usage: /resadmin repossess <unitId>"));
                    return true;
                }

                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.setLeaseState(LeaseState.ESCROW_OPEN);
                rec.setLeaseEnd(Instant.now());
                rec.setTenantPlayerId(null);
                rec.getManagerIds().clear();
                manager.getRepository().saveTenancy(rec);
                manager.getRepository().saveEscrow(new EscrowRecord(args[1], Instant.now(), "Manual repossession", "OPEN"));
                sender.sendMessage(Message.ok("Unit repossessed into escrow."));
                return true;
            }

            case "escrow" -> {
                if (args.length < 2) {
                    sender.sendMessage(Message.error("Usage: /resadmin escrow <unitId>"));
                    return true;
                }

                EscrowRecord escrow = manager.getRepository().getEscrow(args[1]);
                sender.sendMessage(escrow == null
                        ? Message.warn("No escrow record.")
                        : Message.info("Escrow: " + escrow.getStatus() + " reason=" + escrow.getReason()));
                return true;
            }

            case "addmanager" -> {
                if (args.length < 3) {
                    sender.sendMessage(Message.error("Usage: /resadmin addmanager <unitId> <player>"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                manager.getRepository().addRoleAssignment(
                        new RoleAssignment(args[1], RoleType.MANAGER, target.getUniqueId(), null, Instant.now(), null, "admin grant")
                );

                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.getManagerIds().add(target.getUniqueId());
                manager.getRepository().saveTenancy(rec);
                sender.sendMessage(Message.ok("Manager added."));
                return true;
            }

            case "wand" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }

                player.getInventory().addItem(AdminSelectionManager.createWandItem());
                player.sendMessage(Message.ok("You received the Residency iron shovel wand."));
                player.sendMessage(Message.info("Left-click a block for pos1, right-click for pos2."));
                return true;
            }

            case "selection" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }

                SelectionSession session = selectionManager.getSession(player.getUniqueId());
                sender.sendMessage(Message.info("Selection status:"));
                sender.sendMessage(" pos1 = " + format(session.getPos1()));
                sender.sendMessage(" pos2 = " + format(session.getPos2()));
                return true;
            }

            case "clearselection" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }

                selectionManager.clear(player.getUniqueId());
                sender.sendMessage(Message.ok("Selection cleared."));
                return true;
            }

            case "createunit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }

                if (args.length < 7) {
                    sender.sendMessage(Message.error("Usage: /resadmin createunit <id> <district> <mode> <type> <pricingProfile> <policyProfile> [building]"));
                    return true;
                }

                SelectionSession session = selectionManager.getSession(player.getUniqueId());
                if (!session.isComplete()) {
                    sender.sendMessage(Message.error("Selection incomplete. Use the iron shovel to set pos1 and pos2 first."));
                    return true;
                }

                String id = args[1];
                String districtId = args[2];
                String modeRaw = args[3];
                String typeRaw = args[4];
                String pricingProfile = args[5];
                String policyProfile = args[6];
                String buildingId = args.length >= 8 ? args[7] : null;

                UnitMode mode;
                UnitType type;
                try {
                    mode = UnitMode.valueOf(modeRaw.toUpperCase());
                    type = UnitType.valueOf(typeRaw.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Message.error("Invalid mode or type."));
                    return true;
                }

                if (manager.getDistrictsConfigManager().getDistrict(districtId) == null) {
                    sender.sendMessage(Message.error("Unknown district."));
                    return true;
                }

                manager.getUnitsConfigManager().saveNewUnitFromSelection(
                        id,
                        districtId,
                        buildingId,
                        mode,
                        type,
                        pricingProfile,
                        policyProfile,
                        session.getPos1(),
                        session.getPos2()
                );

                manager.initialize();
                sender.sendMessage(Message.ok("Unit " + id + " saved to units.yml and reloaded."));
                return true;
            }

            case "addsubregion" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(Message.error("Usage: /resadmin addsubregion <unitId> <name>"));
                    return true;
                }

                SelectionSession session = selectionManager.getSession(player.getUniqueId());
                if (!session.isComplete()) {
                    sender.sendMessage(Message.error("Selection incomplete. Use the iron shovel to set pos1 and pos2 first."));
                    return true;
                }

                String unitId = args[1];
                String subregionName = args[2];

                if (manager.getUnit(unitId) == null) {
                    sender.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                manager.getUnitsConfigManager().saveLinkedSubregion(unitId, subregionName, session.getPos1(), session.getPos2());
                manager.initialize();
                sender.sendMessage(Message.ok("Linked subregion added and reloaded."));
                return true;
            }

            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Message.info("Residency admin commands:"));
        sender.sendMessage(" /resadmin help");
        sender.sendMessage(" /resadmin reload");
        sender.sendMessage(" /resadmin info <unitId>");
        sender.sendMessage(" /resadmin assign <unitId> <player>");
        sender.sendMessage(" /resadmin terminate <unitId>");
        sender.sendMessage(" /resadmin repossess <unitId>");
        sender.sendMessage(" /resadmin escrow <unitId>");
        sender.sendMessage(" /resadmin addmanager <unitId> <player>");
        sender.sendMessage(" /resadmin wand");
        sender.sendMessage(" /resadmin selection");
        sender.sendMessage(" /resadmin clearselection");
        sender.sendMessage(" /resadmin createunit <id> <district> <mode> <type> <pricingProfile> <policyProfile> [building]");
        sender.sendMessage(" /resadmin addsubregion <unitId> <name>");
    }

    private String format(Location location) {
        if (location == null) return "unset";
        return location.getWorld().getName() + " "
                + location.getBlockX() + ","
                + location.getBlockY() + ","
                + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sunway.residency.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return partial(args[0], List.of(
                    "help", "reload", "info", "assign", "terminate", "repossess", "escrow",
                    "addmanager", "wand", "selection", "clearselection", "createunit", "addsubregion"
            ));
        }

        if (args.length == 2 && List.of("info", "assign", "terminate", "repossess", "escrow", "addmanager", "addsubregion").contains(args[0].toLowerCase())) {
            return partial(args[1], manager.getUnits().values().stream().map(UnitDefinition::getId).sorted().collect(Collectors.toList()));
        }

        if (args.length == 3 && List.of("assign", "addmanager").contains(args[0].toLowerCase())) {
            return partial(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().collect(Collectors.toList()));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("createunit")) {
            return partial(args[2], manager.getDistrictsConfigManager().getDistricts().stream()
                    .map(DistrictDefinition::getId)
                    .sorted()
                    .collect(Collectors.toList()));
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("createunit")) {
            return partial(args[3], Arrays.stream(UnitMode.values()).map(Enum::name).collect(Collectors.toList()));
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("createunit")) {
            return partial(args[4], Arrays.stream(UnitType.values()).map(Enum::name).collect(Collectors.toList()));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String token, List<String> source) {
        String lower = token.toLowerCase();
        return source.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
