package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.city.CityOverviewService;
import com.sunwayMinecraft.city.metrics.CityMetricSnapshot;
import com.sunwayMinecraft.city.metrics.CityMetricsManager;
import com.sunwayMinecraft.city.CityValidationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CityAdminCommands implements CommandExecutor, TabCompleter {
    private final CityMetricsManager metricsManager;
    private final CityValidationService validationService;

    public CityAdminCommands(CityMetricsManager metricsManager, CityValidationService validationService) {
        this.metricsManager = metricsManager;
        this.validationService = validationService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "validate" -> runValidation(sender);
            case "stats" -> showStats(sender);
            case "help" -> showHelp(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void runValidation(CommandSender sender) {
        sender.sendMessage(Component.text("=== City Validation ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        Map<String, List<String>> results = validationService.runAll();
        
        results.forEach((system, issues) -> {
            if (issues.isEmpty()) {
                sender.sendMessage(Component.text(system + ": ", NamedTextColor.YELLOW)
                        .append(Component.text("OK", NamedTextColor.GREEN)));
            } else {
                sender.sendMessage(Component.text(system + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(issues.size() + " issue(s)", NamedTextColor.RED)));
                for (String issue : issues) {
                    sender.sendMessage(Component.text("- " + issue, NamedTextColor.RED));
                }
            }
        });
    }

    private void showStats(CommandSender sender) {
        if (metricsManager == null) {
            sender.sendMessage(Component.text("Metrics system is unavailable.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== City Metrics Stats ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        CityMetricSnapshot snapshot = metricsManager.getSnapshot();
        
        snapshot.metrics().forEach((key, value) -> {
            sender.sendMessage(Component.text(key + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(String.format("%.2f", value), NamedTextColor.WHITE)));
        });
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== City Admin Help ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/cityadmin validate - Run cross-system validation", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cityadmin stats - View city metrics and statistics", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cityadmin help - Show this help message", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("validate", "stats", "help");
        }
        return null;
    }
}
