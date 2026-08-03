package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.service.CityEventsManager;
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
import java.util.List;
import java.util.stream.Collectors;

public class EventsCommands implements CommandExecutor, TabCompleter {
    private final CityEventsManager manager;

    public EventsCommands(CityEventsManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "current" -> showCurrent(sender);
            case "upcoming" -> showUpcoming(sender);
            case "info" -> showInfo(sender, args);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void showCurrent(CommandSender sender) {
        List<ActiveCityEvent> active = manager.getActiveEvents();
        if (active.isEmpty()) {
            sender.sendMessage(Component.text("There are no city events active right now.", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("=== Current City Events ===", NamedTextColor.GOLD));
        active.forEach(ac -> {
            CityEventDefinition def = manager.getConfigManager().getEvent(ac.getEventId());
            if (def != null) {
                long minsLeft = Duration.between(Instant.now(), ac.getEndTime()).toMinutes();
                sender.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.text(def.name(), NamedTextColor.YELLOW))
                    .append(Component.text(" (" + minsLeft + "m left)", NamedTextColor.GRAY)));
            }
        });
    }

    private void showUpcoming(CommandSender sender) {
        sender.sendMessage(Component.text("Check back later for scheduled upcoming events!", NamedTextColor.YELLOW));
    }

    private void showInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /events info <id>", NamedTextColor.RED));
            return;
        }
        CityEventDefinition def = manager.getConfigManager().getEvent(args[1]);
        if (def == null) {
            sender.sendMessage(Component.text("Event not found.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Event: " + def.name() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW).append(Component.text(def.description(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Multiplier: ", NamedTextColor.YELLOW).append(Component.text("x" + def.rewardMultiplier(), NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Boosts: ", NamedTextColor.YELLOW).append(Component.text(
            def.boostedCategories().stream().map(Enum::name).collect(Collectors.joining(", ")), NamedTextColor.WHITE)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== City Events Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/events current - View active events", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/events upcoming - View scheduled events", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/events info <id> - Detailed event info", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("current", "upcoming", "info", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return List.copyOf(manager.getConfigManager().getEvents().keySet());
        }
        return null;
    }
}
