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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class EventAdminCommands implements CommandExecutor, TabCompleter {
    private final CityEventsManager manager;

    public EventAdminCommands(CityEventsManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("sunway.events.admin")) {
            sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listDefinitions(sender);
            case "active" -> showActive(sender);
            case "start" -> startEvent(sender, args);
            case "stop" -> stopEvent(sender, args);
            case "reload" -> {
                manager.getConfigManager().load();
                manager.getSettingsManager().load();
                sender.sendMessage(Component.text("City Events configuration reloaded.", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void listDefinitions(CommandSender sender) {
        sender.sendMessage(Component.text("=== Event Definitions ===", NamedTextColor.GOLD));
        manager.getConfigManager().getEvents().keySet().forEach(id -> 
            sender.sendMessage(Component.text("- " + id, NamedTextColor.YELLOW)));
    }

    private void showActive(CommandSender sender) {
        List<ActiveCityEvent> active = manager.getActiveEvents();
        if (active.isEmpty()) {
            sender.sendMessage(Component.text("No active events.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("=== Active Events ===", NamedTextColor.GOLD));
        active.forEach(ac -> {
            long minsLeft = Duration.between(Instant.now(), ac.getEndTime()).toMinutes();
            sender.sendMessage(Component.text("- " + ac.getEventId() + " (" + minsLeft + "m left)", NamedTextColor.YELLOW));
        });
    }

    private void startEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /eventadmin start <id> [durationMinutes]", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        CityEventDefinition def = manager.getConfigManager().getEvent(id);
        if (def == null) {
            sender.sendMessage(Component.text("Event definition not found.", NamedTextColor.RED));
            return;
        }

        long duration = def.defaultDurationMinutes();
        if (args.length >= 3) {
            try {
                duration = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid duration. Using default.", NamedTextColor.YELLOW));
            }
        }

        if (manager.startEvent(id, duration, ActiveCityEvent.TriggerMode.ADMIN)) {
            sender.sendMessage(Component.text("Event " + id + " started for " + duration + " minutes.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not start event. (Limit reached or already active?)", NamedTextColor.RED));
        }
    }

    private void stopEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /eventadmin stop <id>", NamedTextColor.RED));
            return;
        }
        if (manager.stopEvent(args[1])) {
            sender.sendMessage(Component.text("Event " + args[1] + " stopped.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Event not active.", NamedTextColor.RED));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Event Admin Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/eventadmin list - List all event definitions", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eventadmin active - Show currently active events", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eventadmin start <id> [mins] - Manually start an event", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eventadmin stop <id> - Manually stop an active event", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eventadmin reload - Reload configs", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "active", "start", "stop", "reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start")) {
                return List.copyOf(manager.getConfigManager().getEvents().keySet());
            }
            if (args[0].equalsIgnoreCase("stop")) {
                return manager.getActiveEvents().stream().map(ActiveCityEvent::getEventId).collect(Collectors.toList());
            }
        }
        return null;
    }
}
