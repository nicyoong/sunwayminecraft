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
