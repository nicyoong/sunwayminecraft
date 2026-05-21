package com.sunwayMinecraft.events.service;

import com.sunwayMinecraft.events.config.EventConfigManager;
import com.sunwayMinecraft.events.config.EventSettingsManager;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.persistence.EventPersistenceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CityEventsManager {
    private final JavaPlugin plugin;
    private final EventConfigManager configManager;
    private final EventSettingsManager settingsManager;
    private final EventPersistenceService persistence;
    private final List<ActiveCityEvent> activeEvents = new ArrayList<>();

    public CityEventsManager(JavaPlugin plugin, EventConfigManager configManager, 
                             EventSettingsManager settingsManager, EventPersistenceService persistence) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.settingsManager = settingsManager;
        this.persistence = persistence;
    }

    public void initialize() {
        activeEvents.addAll(persistence.load());
        startCleanupTask();
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = activeEvents.removeIf(ac -> {
                if (ac.isExpired()) {
                    announceEnd(ac);
                    return true;
                }
                return false;
            });
            if (changed) persistence.save(activeEvents);
        }, 20 * 60, 20 * 60); // Check every minute
    }

    public boolean startEvent(String eventId, long durationMinutes, ActiveCityEvent.TriggerMode mode) {
        CityEventDefinition def = configManager.getEvent(eventId);
        if (def == null) return false;
        
        if (activeEvents.size() >= settingsManager.getMaxActiveEvents()) return false;
        if (isEventActive(eventId)) return false;

        Instant end = Instant.now().plus(Duration.ofMinutes(durationMinutes));
        ActiveCityEvent ac = new ActiveCityEvent(eventId, Instant.now(), end, mode);
        activeEvents.add(ac);
        persistence.save(activeEvents);
        announceStart(ac);
        return true;
    }

    public boolean stopEvent(String eventId) {
        Optional<ActiveCityEvent> ac = activeEvents.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .findFirst();
        
        if (ac.isPresent()) {
            activeEvents.remove(ac.get());
            persistence.save(activeEvents);
            announceEnd(ac.get());
            return true;
        }
        return false;
    }

    private void announceStart(ActiveCityEvent ac) {
        if (!settingsManager.isAutoAnnouncements()) return;
        CityEventDefinition def = configManager.getEvent(ac.getEventId());
        if (def == null) return;

        Component message = Component.text("City Event Started: ", NamedTextColor.GOLD)
            .append(Component.text(def.name(), NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text(def.description(), NamedTextColor.GRAY));
        
        Bukkit.broadcast(message);
    }

    private void announceEnd(ActiveCityEvent ac) {
        if (!settingsManager.isAutoAnnouncements()) return;
        CityEventDefinition def = configManager.getEvent(ac.getEventId());
        if (def == null) return;

        Component message = Component.text("City Event Ended: ", NamedTextColor.GOLD)
            .append(Component.text(def.name(), NamedTextColor.YELLOW));
        
        Bukkit.broadcast(message);
    }

    public boolean isEventActive(String eventId) {
        return activeEvents.stream().anyMatch(e -> e.getEventId().equals(eventId));
    }

    public List<ActiveCityEvent> getActiveEvents() { return activeEvents; }
    public EventConfigManager getConfigManager() { return configManager; }
    public EventSettingsManager getSettingsManager() { return settingsManager; }
}
