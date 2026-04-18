package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ResidencyCommands implements TabExecutor {
    private final ResidencyManager manager;
    private final Map<UUID, String> pendingLeaveConfirm = new HashMap<>();

    public ResidencyCommands(ResidencyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                player.sendMessage(Message.info("Available units:"));
                List<UnitDefinition> available = manager.getDirectoryService().listAvailableUnits();
                if (available.isEmpty()) {
                    player.sendMessage(Message.warn("No units are currently available."));
                    return true;
                }

                for (UnitDefinition unit : available) {
                    var pricing = manager.getPricingProfile(unit);
                    String priceText = pricing == null ? "?" : String.valueOf(pricing.getBaseRent());
                    player.sendMessage(" - " + unit.getId() + " [" + unit.getMode() + "] rent=" + priceText);
                }
                return true;
            }

            case "view" -> {
                if (args.length < 2) {
                    player.sendMessage(Message.error("Usage: /residency view <unitId>"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    player.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                var pricing = manager.getPricingProfile(unit);
                UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());

                player.sendMessage(Message.info(unit.getDisplayName() + " [" + unit.getId() + "]"));
                player.sendMessage(" Address: " + unit.getAddressLine());
                player.sendMessage(" District: " + unit.getDistrictId());
                player.sendMessage(" Mode: " + unit.getMode() + ", Type: " + unit.getUnitType());
                player.sendMessage(" Rent: " + (pricing == null ? "?" : pricing.getBaseRent())
                        + ", Deposit: " + (pricing == null ? "?" : pricing.getDeposit()));
                player.sendMessage(" State: " + record.getLeaseState());
                player.sendMessage(" Approval Required: " + unit.getListingSettings().isApprovalRequired());
                return true;
            }

            case "rent" -> {
                if (args.length < 2) {
                    player.sendMessage(Message.error("Usage: /residency rent <unitId>"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    player.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
                if (!(record.getLeaseState() == LeaseState.VACANT || record.getLeaseState() == LeaseState.LISTED)) {
                    player.sendMessage(Message.error("This unit is not available."));
                    return true;
                }

                boolean ok = manager.getBillingService().startLease(unit, player, unit.getListingSettings().isApprovalRequired());
                if (!ok) {
                    player.sendMessage(Message.error("You could not rent this unit. Check your funds or approval status."));
                    return true;
                }

                player.sendMessage(Message.ok("You rented " + unit.getDisplayName() + "."));
                player.sendMessage(Message.info("Use /residency myunits to see your current rentals."));
                return true;
            }

            case "pay" -> {
                if (args.length < 2) {
                    player.sendMessage(Message.error("Usage: /residency pay <unitId>"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    player.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                boolean ok = manager.getBillingService().payRent(unit, player);
                player.sendMessage(ok ? Message.ok("Rent paid.") : Message.error("Rent payment failed."));
                return true;
            }

            case "myunits" -> {
                player.sendMessage(Message.info("Your units:"));
                boolean found = false;
                for (UnitDefinition unit : manager.getUnits().values()) {
                    UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
                    if (player.getUniqueId().equals(record.getTenantPlayerId())
                            || record.getManagerIds().contains(player.getUniqueId())) {
                        found = true;
                        player.sendMessage(" - " + unit.getDisplayName() + " [" + unit.getId() + "] " + record.getLeaseState());
                    }
                }

                if (!found) {
                    player.sendMessage(Message.warn("You do not currently rent or manage any units."));
                }
                return true;
            }

            case "guest", "guests" -> {
                if (args.length < 3) {
                    player.sendMessage(Message.error("Usage: /residency guest <unitId> <player> [hours]"));
                    return true;
                }

                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) {
                    player.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
                if (!player.getUniqueId().equals(record.getTenantPlayerId())
                        && !record.getManagerIds().contains(player.getUniqueId())
                        && !player.hasPermission("sunway.residency.admin")) {
                    player.sendMessage(Message.error("You do not manage this unit."));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                long hours = args.length >= 4 ? Long.parseLong(args[3]) : 24L;

                manager.getRepository().addGuestAccess(
                        manager.createGuestGrant(unit.getId(), target.getUniqueId(), hours, player.getUniqueId())
                );

                player.sendMessage(Message.ok("Guest access granted to " + target.getName() + " for " + hours + " hours."));
                return true;
            }

            case "leave" -> {
                if (args.length < 2) {
                    player.sendMessage(Message.error("Usage: /residency leave <unitId> [confirm]"));
                    return true;
                }

                String unitId = args[1];
                UnitDefinition unit = manager.getUnit(unitId);
                if (unit == null) {
                    player.sendMessage(Message.error("Unknown unit."));
                    return true;
                }

                UnitTenancyRecord record = manager.getRepository().getTenancy(unitId);
                if (record.getTenantPlayerId() == null || !record.getTenantPlayerId().equals(player.getUniqueId())) {
                    player.sendMessage(Message.error("You are not the tenant of that unit."));
                    return true;
                }

                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    pendingLeaveConfirm.put(player.getUniqueId(), unitId.toLowerCase());
                    player.sendMessage(Message.warn("You are about to leave " + unit.getDisplayName() + "."));
                    player.sendMessage(Message.warn("This will end your lease and revoke your access."));
                    player.sendMessage(Message.info("Run /residency leave " + unitId + " confirm to continue."));
                    return true;
                }

                String pending = pendingLeaveConfirm.get(player.getUniqueId());
                if (pending == null || !pending.equalsIgnoreCase(unitId)) {
                    player.sendMessage(Message.error("No pending leave confirmation for that unit. Run the command again first."));
                    return true;
                }

                boolean ok = manager.getBillingService().terminateLease(unit, player);
                pendingLeaveConfirm.remove(player.getUniqueId());

                if (!ok) {
                    player.sendMessage(Message.error("Could not terminate the lease."));
                    return true;
                }

                player.sendMessage(Message.ok("You have stopped renting " + unit.getDisplayName() + "."));
                return true;
            }

            default -> {
                sendHelp(player);
                return true;
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Message.info("Residency commands:"));
        player.sendMessage(" /residency list");
        player.sendMessage(" /residency view <unitId>");
        player.sendMessage(" /residency rent <unitId>");
        player.sendMessage(" /residency myunits");
        player.sendMessage(" /residency pay <unitId>");
        player.sendMessage(" /residency guest <unitId> <player> [hours]");
        player.sendMessage(" /residency leave <unitId> [confirm]");
        player.sendMessage(" /residency help");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return partial(args[0], List.of("help", "list", "view", "rent", "myunits", "pay", "guest", "leave"));
        }

        if (args.length == 2 && List.of("view", "rent", "pay", "guest", "leave").contains(args[0].toLowerCase())) {
            return partial(args[1], manager.getUnits().values().stream()
                    .map(UnitDefinition::getId)
                    .sorted()
                    .collect(Collectors.toList()));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("guest")) {
            return partial(args[2], Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted()
                    .collect(Collectors.toList()));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("leave")) {
            return partial(args[2], List.of("confirm"));
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
