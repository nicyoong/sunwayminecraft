package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.containerfinder.ContainerFinderManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /findcontainers commands.
 *
 * Supported:
 * - /findcontainers
 * - /findcontainers <radius>
 * - /findcontainers page <n>
 */
public class ContainerFinderCommands implements CommandExecutor {
    private final ContainerFinderManager containerFinder;

    public ContainerFinderCommands(ContainerFinderManager containerFinder) {
        this.containerFinder = containerFinder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("findcontainers")) {
            return false;
        }

        if (!sender.hasPermission("containerfinder.use")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("page")) {
            handlePageCommand(sender, args);
            return true;
        }

        handleScanCommand(player, args);
        return true;
    }

    private void handleScanCommand(Player player, String[] args) {
        int radius = containerFinder.getDefaultRadius();

        if (args.length > 1) {
            senderUsage(player);
            return;
        }

        if (args.length == 1) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cRadius must be a number.");
                return;
            }

            if (radius <= 0) {
                player.sendMessage("§cRadius must be greater than 0.");
                return;
            }
        }

        containerFinder.startSearch(player, radius, false);
    }

    private void handlePageCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /findcontainers page <number>");
            return;
        }

        int page;
        try {
            page = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cPage must be a number.");
            return;
        }

        if (page <= 0) {
            sender.sendMessage("§cPage must be greater than 0.");
            return;
        }

        containerFinder.sendPage(sender, page);
    }

    private void senderUsage(CommandSender sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§7/findcontainers");
        sender.sendMessage("§7/findcontainers <radius>");
        sender.sendMessage("§7/findcontainers page <number>");
    }
}