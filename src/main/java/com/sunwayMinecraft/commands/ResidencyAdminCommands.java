package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.EscrowRecord;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.RoleAssignment;
import com.sunwayMinecraft.residency.domain.RoleType;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Instant;

public class ResidencyAdminCommands implements CommandExecutor {
    private final ResidencyManager manager;

    public ResidencyAdminCommands(ResidencyManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunway.residency.admin")) { sender.sendMessage(Message.error("No permission.")); return true; }
        if (args.length == 0) { sender.sendMessage(Message.error("/resadmin <reload|info|assign|terminate|repossess|escrow|addmanager> ...")); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.initialize();
                sender.sendMessage(Message.ok("Residency configs reloaded."));
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage(Message.error("/resadmin info <unitId>")); return true; }
                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) { sender.sendMessage(Message.error("Unknown unit.")); return true; }
                UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
                sender.sendMessage(Message.info(unit.getDisplayName() + " [" + unit.getId() + "]"));
                sender.sendMessage(" District: " + unit.getDistrictId() + ", Building: " + unit.getBuildingId());
                sender.sendMessage(" Lease state: " + record.getLeaseState() + ", Rent state: " + record.getRentState());
                sender.sendMessage(" Tenant: " + (record.getTenantPlayerId() == null ? "none" : record.getTenantPlayerId()));
            }
            case "assign" -> {
                if (args.length < 3) { sender.sendMessage(Message.error("/resadmin assign <unitId> <player>")); return true; }
                UnitDefinition unit = manager.getUnit(args[1]);
                if (unit == null) { sender.sendMessage(Message.error("Unknown unit.")); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                boolean ok = manager.getBillingService().startLease(unit, target, unit.getListingSettings().isApprovalRequired());
                sender.sendMessage(ok ? Message.ok("Lease assigned.") : Message.error("Could not assign lease."));
            }
            case "terminate" -> {
                if (args.length < 2) { sender.sendMessage(Message.error("/resadmin terminate <unitId>")); return true; }
                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.setLeaseState(LeaseState.REPOSSESSED);
                rec.setLeaseEnd(Instant.now());
                rec.setTenantPlayerId(null);
                rec.getManagerIds().clear();
                manager.getRepository().saveTenancy(rec);
                sender.sendMessage(Message.ok("Lease terminated."));
            }
            case "repossess" -> {
                if (args.length < 2) { sender.sendMessage(Message.error("/resadmin repossess <unitId>")); return true; }
                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.setLeaseState(LeaseState.ESCROW_OPEN);
                rec.setLeaseEnd(Instant.now());
                rec.setTenantPlayerId(null);
                rec.getManagerIds().clear();
                manager.getRepository().saveTenancy(rec);
                manager.getRepository().saveEscrow(new EscrowRecord(args[1], Instant.now(), "Manual repossession", "OPEN"));
                sender.sendMessage(Message.ok("Unit repossessed into escrow."));
            }
            case "escrow" -> {
                if (args.length < 2) { sender.sendMessage(Message.error("/resadmin escrow <unitId>")); return true; }
                EscrowRecord escrow = manager.getRepository().getEscrow(args[1]);
                sender.sendMessage(escrow == null ? Message.warn("No escrow record.") : Message.info("Escrow: " + escrow.getStatus() + " reason=" + escrow.getReason()));
            }
            case "addmanager" -> {
                if (args.length < 3) { sender.sendMessage(Message.error("/resadmin addmanager <unitId> <player>")); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                manager.getRepository().addRoleAssignment(new RoleAssignment(args[1], RoleType.MANAGER, target.getUniqueId(), null, Instant.now(), null, "admin grant"));
                UnitTenancyRecord rec = manager.getRepository().getTenancy(args[1]);
                rec.getManagerIds().add(target.getUniqueId());
                manager.getRepository().saveTenancy(rec);
                sender.sendMessage(Message.ok("Manager added."));
            }
            default -> sender.sendMessage(Message.error("Unknown subcommand."));
        }
        return true;
    }
}
