package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.contracts.service.ContractVerificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractsCommands implements CommandExecutor, TabCompleter {
    private final ContractsManager manager;
    private final ContractVerificationService verificationService;

    public ContractsCommands(ContractsManager manager, ContractVerificationService verificationService) {
        this.manager = manager;
        this.verificationService = verificationService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use contract commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "board" -> showBoard(player);
            case "list" -> listAvailable(player);
            case "accept" -> acceptContract(player, args);
            case "active" -> listActive(player);
            case "info" -> showInfo(player, args);
            case "complete" -> completeContract(player, args);
            case "abandon" -> abandonContract(player, args);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void showBoard(Player player) {
        player.sendMessage(Component.text("=== City Contracts Board ===", NamedTextColor.GOLD));
        manager.getContractConfig().getContracts().values().forEach(def -> {
            player.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                .append(Component.text(def.name(), NamedTextColor.YELLOW))
                .append(Component.text(" (ID: " + def.id() + ")", NamedTextColor.DARK_GRAY)));
        });
        player.sendMessage(Component.text("Use /contracts info <id> for details.", NamedTextColor.GRAY));
    }

    private void listAvailable(Player player) {
        showBoard(player);
    }

    private void acceptContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts accept <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        if (manager.acceptContract(player, id)) {
            player.sendMessage(Component.text("Contract accepted: " + id, NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to accept contract. Check limits or cooldowns.", NamedTextColor.RED));
        }
    }

    private void listActive(Player player) {
        List<ActiveContract> active = manager.getPersistence().getPlayerContracts(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(Component.text("You have no active contracts.", NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("=== Your Active Contracts ===", NamedTextColor.GOLD));
        active.forEach(ac -> {
            ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
            long minsLeft = Duration.between(Instant.now(), ac.getExpiryTime()).toMinutes();
            player.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                .append(Component.text(def != null ? def.name() : ac.getContractId(), NamedTextColor.YELLOW))
                .append(Component.text(" (" + Math.max(0, minsLeft) + "m left)", NamedTextColor.GRAY)));
        });
    }

    private void showInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts info <id>", NamedTextColor.RED));
            return;
        }
        ContractDefinition def = manager.getContractConfig().getContract(args[1]);
        if (def == null) {
            player.sendMessage(Component.text("Contract not found.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("=== Contract: " + def.name() + " ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW).append(Component.text(def.description(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Reward: ", NamedTextColor.YELLOW).append(Component.text("$" + def.rewardMoney(), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Objective: ", NamedTextColor.YELLOW).append(Component.text(def.objectiveDescription(), NamedTextColor.WHITE)));
    }

    private void completeContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts complete <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        ActiveContract ac = manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
            .filter(a -> a.getContractId().equals(id)).findFirst().orElse(null);

        if (ac == null) {
            player.sendMessage(Component.text("You don't have this contract active.", NamedTextColor.RED));
            return;
        }

        ContractVerificationService.VerificationResult result = verificationService.verifyCompletion(player, ac);
        if (result.success()) {
            if (manager.completeContract(player, ac)) {
                player.sendMessage(Component.text("Contract completed! Reward issued.", NamedTextColor.GREEN));
            }
        } else {
            player.sendMessage(Component.text("Cannot complete: " + result.message(), NamedTextColor.RED));
        }
    }

    private void abandonContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts abandon <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        ActiveContract ac = manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
            .filter(a -> a.getContractId().equals(id)).findFirst().orElse(null);

        if (ac == null) {
            player.sendMessage(Component.text("You don't have this contract active.", NamedTextColor.RED));
            return;
        }

        manager.abandonContract(player, ac);
        player.sendMessage(Component.text("Contract abandoned. Cooldown applied.", NamedTextColor.YELLOW));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== City Contracts Help ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/contracts board - View available contracts", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts info <id> - View contract details", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts accept <id> - Accept a contract", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts active - List your active contracts", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts complete <id> - Complete a contract", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts abandon <id> - Abandon a contract", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return null;
        if (args.length == 1) {
            return List.of("board", "list", "accept", "active", "info", "complete", "abandon", "help");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("info")) {
                return new ArrayList<>(manager.getContractConfig().getContracts().keySet());
            }
            if (args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("abandon")) {
                return manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
                    .map(ActiveContract::getContractId).collect(Collectors.toList());
            }
        }
        return null;
    }
}
