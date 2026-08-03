package com.sunwayMinecraft.events.persistence;

import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class EventPersistenceService {
    private final JavaPlugin plugin;
    private final File dataFile;

    public EventPersistenceService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "city-event-state.yml");
    }

    public List<ActiveCityEvent> load() {
        List<ActiveCityEvent> list = new ArrayList<>();
        if (!dataFile.exists()) return list;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("active_events");
        if (section == null) return list;

        for (String key : section.getKeys(false)) {
            try {
                String eventId = section.getString(key + ".id");
                Instant start = Instant.parse(section.getString(key + ".start"));
                Instant end = Instant.parse(section.getString(key + ".end"));
                ActiveCityEvent.TriggerMode mode = ActiveCityEvent.TriggerMode.valueOf(section.getString(key + ".mode"));
                
                ActiveCityEvent active = new ActiveCityEvent(eventId, start, end, mode);
                if (!active.isExpired()) {
                    list.add(active);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load active event: " + key, e);
            }
        }
        return list;
    }

    public void save(List<ActiveCityEvent> activeEvents) {
        YamlConfiguration config = new YamlConfiguration();
        int i = 0;
        for (ActiveCityEvent ac : activeEvents) {
            String path = "active_events." + (i++);
            config.set(path + ".id", ac.getEventId());
            config.set(path + ".start", ac.getStartTime().toString());
            config.set(path + ".end", ac.getEndTime().toString());
            config.set(path + ".mode", ac.getTriggerMode().name());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save active events state!", e);
        }
    }
}
