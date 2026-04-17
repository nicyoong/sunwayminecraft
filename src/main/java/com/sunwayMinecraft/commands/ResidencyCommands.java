package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResidencyCommands implements CommandExecutor {
    private final ResidencyManager manager;

    public ResidencyCommands(ResidencyManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            player.sendMessage(Message.info("Available units:"));
            for (UnitDefinition unit : manager.getDirectoryService().listAvailableUnits()) {
                var pricing = manager.getPricingProfile(unit);
                player.sendMessage(" - " + unit.getId() + " [" + unit.getMode() + "] rent=" + (pricing == null ? "?" : pricing.getBaseRent()));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) { player.sendMessage(Message.error("/residency view <unitId>")); return true; }
            UnitDefinition unit = manager.getUnit(args[1]);
            if (unit == null) { player.sendMessage(Message.error("Unknown unit.")); return true; }
            var pricing = manager.getPricingProfile(unit);
            UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
            player.sendMessage(Message.info(unit.getDisplayName() + " [" + unit.getId() + "]"));
            player.sendMessage(" Address: " + unit.getAddressLine());
            player.sendMessage(" Mode: " + unit.getMode() + ", Type: " + unit.getUnitType());
            player.sendMessage(" Rent: " + (pricing == null ? "?" : pricing.getBaseRent()) + ", Deposit: " + (pricing == null ? "?" : pricing.getDeposit()));
            player.sendMessage(" State: " + record.getLeaseState());
            return true;
        }
        if (args[0].equalsIgnoreCase("rent")) {
            if (args.length < 2) { player.sendMessage(Message.error("/residency rent <unitId>")); return true; }
            UnitDefinition unit = manager.getUnit(args[1]);
            if (unit == null) { player.sendMessage(Message.error("Unknown unit.")); return true; }
            UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
            if (!(record.getLeaseState() == LeaseState.VACANT || record.getLeaseState() == LeaseState.LISTED)) {
                player.sendMessage(Message.error("This unit is not available.")); return true;
            }
            boolean ok = manager.getBillingService().startLease(unit, player, unit.getListingSettings().isApprovalRequired());
            player.sendMessage(ok ? Message.ok("You rented " + unit.getDisplayName() + ".") : Message.error("You could not rent this unit. Check approval or funds."));
            return true;
        }
        if (args[0].equalsIgnoreCase("pay")) {
            if (args.length < 2) { player.sendMessage(Message.error("/residency pay <unitId>")); return true; }
            UnitDefinition unit = manager.getUnit(args[1]);
            if (unit == null) { player.sendMessage(Message.error("Unknown unit.")); return true; }
            boolean ok = manager.getBillingService().payRent(unit, player);
            player.sendMessage(ok ? Message.ok("Rent paid.") : Message.error("Rent payment failed."));
            return true;
        }
