package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;

public class StorefrontCommands implements TabExecutor {
    private final ResidencyManager manager;

    public StorefrontCommands(ResidencyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(Message.info("Storefront commands:"));
            sender.sendMessage(" /storefront list");
            sender.sendMessage(" /storefront help");
            sender.sendMessage(Message.info("Available storefronts:"));

            boolean found = false;
            for (UnitDefinition unit : manager.getDirectoryService().listAvailableByMode("COMMERCIAL")) {
                sender.sendMessage(" - " + unit.getDisplayName() + " [" + unit.getId() + "]");
                found = true;
            }
            for (UnitDefinition unit : manager.getDirectoryService().listAvailableByMode("MIXED_USE")) {
                sender.sendMessage(" - " + unit.getDisplayName() + " [" + unit.getId() + "]");
                found = true;
            }

            if (!found) {
                sender.sendMessage(Message.warn("No storefronts are currently available."));
            }
            return true;
        }

        sender.sendMessage(Message.error("Unknown subcommand. Try /storefront help"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "list");
        }
        return Collections.emptyList();
    }
}
