package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.city.CityOverviewService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CityCommands implements CommandExecutor, TabCompleter {
    private final CityOverviewService overviewService;

    public CityCommands(CityOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use city commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "guide" -> showGuide(player);
            case "help" -> showHelp(player);
            default -> showHelp(player);
        }

        return true;
    }

    private void showStatus(Player player) {
        player.sendMessage(Component.text("=== City Overview ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        player.sendMessage(Component.text("Active Event: ", NamedTextColor.YELLOW)
                .append(Component.text(overviewService.getCurrentEventName(), NamedTextColor.WHITE)));
        
        int available = overviewService.getAvailableContractsCount();
        int boosted = overviewService.getBoostedContractsCount();
        Component contractComp = Component.text(available + " available", NamedTextColor.WHITE);
        if (boosted > 0) {
            contractComp = contractComp.append(Component.text(", " + boosted + " boosted", NamedTextColor.AQUA));
        }
        player.sendMessage(Component.text("Contracts: ", NamedTextColor.YELLOW).append(contractComp));
        
        player.sendMessage(Component.text("District: ", NamedTextColor.YELLOW)
                .append(Component.text(overviewService.getDistrictName(player), NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("Residency: ", NamedTextColor.YELLOW)
                .append(Component.text(overviewService.getResidencySummary(player), NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("Mining World: ", NamedTextColor.YELLOW)
                .append(Component.text("Use /mineworld for resources", NamedTextColor.GRAY)));

        player.sendMessage(Component.text("\nUseful Commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("- /contracts board", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("- /events current", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("- /district", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("- /residency list", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("- /storefront list", NamedTextColor.YELLOW));
    }

    private void showGuide(Player player) {
        player.sendMessage(Component.text("=== New Player Guide ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Suggested first steps:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("1. Use ", NamedTextColor.WHITE)
                .append(Component.text("/events current", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("2. Use ", NamedTextColor.WHITE)
                .append(Component.text("/contracts board", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("3. Take a hauling contract", NamedTextColor.WHITE));
        player.sendMessage(Component.text("4. Gather resources from survival or mining world", NamedTextColor.WHITE));
        player.sendMessage(Component.text("5. Complete the contract for city pay", NamedTextColor.WHITE));
        player.sendMessage(Component.text("6. Check ", NamedTextColor.WHITE)
                .append(Component.text("/residency list", NamedTextColor.AQUA))
                .append(Component.text(" for city housing", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("7. Use ", NamedTextColor.WHITE)
                .append(Component.text("/district", NamedTextColor.AQUA))
                .append(Component.text(" to learn where you are", NamedTextColor.WHITE)));
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== City Help ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/city status - View current city state", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/city guide - New player quick start guide", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/city help - Show this help message", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("status", "guide", "help");
        }
        return null;
    }
}
