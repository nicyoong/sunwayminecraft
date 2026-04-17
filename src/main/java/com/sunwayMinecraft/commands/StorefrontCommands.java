package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StorefrontCommands implements CommandExecutor {
    private final ResidencyManager manager;

    public StorefrontCommands(ResidencyManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Message.info("Available storefronts:"));
        for (UnitDefinition unit : manager.getDirectoryService().listAvailableByMode("COMMERCIAL")) sender.sendMessage(" - " + unit.getDisplayName() + " [" + unit.getId() + "]");
        for (UnitDefinition unit : manager.getDirectoryService().listAvailableByMode("MIXED_USE")) sender.sendMessage(" - " + unit.getDisplayName() + " [" + unit.getId() + "]");
        return true;
    }
}
