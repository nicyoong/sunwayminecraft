package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ContractAdminCommands implements CommandExecutor, TabCompleter {
    private final ContractsManager manager;

    public ContractAdminCommands(ContractsManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("sunway.contracts.admin")) {
            sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.getContractConfig().load();
                manager.getEndpointConfig().load();
                manager.getSettingsConfig().load();
                sender.sendMessage(Component.text("Contract configurations reloaded.", NamedTextColor.GREEN));
            }
            case "active" -> showPlayerActive(sender, args);
            case "resetcooldown" -> resetCooldown(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void showPlayerActive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /contractadmin active <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        List<ActiveContract> active = manager.getPersistence().getPlayerContracts(target.getUniqueId());
        sender.sendMessage(Component.text("Active contracts for " + target.getName() + ":", NamedTextColor.GOLD));
        active.forEach(ac -> sender.sendMessage(Component.text("- " + ac.getContractId(), NamedTextColor.YELLOW)));
    }

    private void resetCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /contractadmin resetcooldown <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        manager.getPersistence().getPlayerCooldowns(target.getUniqueId()).clear();
        manager.getPersistence().save();
        sender.sendMessage(Component.text("Cooldowns reset for " + target.getName(), NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Contract Admin Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/contractadmin reload - Reload configs", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/contractadmin active <player> - View player's active contracts", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/contractadmin resetcooldown <player> - Reset player's cooldowns", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "active", "resetcooldown");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("active") || args[0].equalsIgnoreCase("resetcooldown"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }
}
