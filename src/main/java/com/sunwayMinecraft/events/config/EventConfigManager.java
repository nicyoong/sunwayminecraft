package com.sunwayMinecraft.events.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class EventConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    private final Map<String, CityEventDefinition> events = new HashMap<>();

    public EventConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "city-events.yml");
        if (!configFile.exists()) {
            plugin.saveResource("city-events.yml", false);
        }
        load();
    }

    public void load() {
        events.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("events");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                CityEventType type = CityEventType.valueOf(section.getString(key + ".type"));
                String name = section.getString(key + ".name");
                String description = section.getString(key + ".description");
                EventScope scope = EventScope.valueOf(section.getString(key + ".scope"));
                double multiplier = section.getDouble(key + ".reward_multiplier", 1.0);
                long duration = section.getLong(key + ".default_duration_minutes", 60);

                Set<ContractCategory> boosted = new HashSet<>();
                List<String> boostedStrs = section.getStringList(key + ".boosted_categories");
                for (String s : boostedStrs) {
                    try {
                        boosted.add(ContractCategory.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid category boost in event " + key + ": " + s);
                    }
                }

                events.put(key, new CityEventDefinition(
                    key, type, name, description, scope, multiplier, boosted, duration
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load city event: " + key, e);
            }
        }
    }

    public Map<String, CityEventDefinition> getEvents() { return events; }
    public CityEventDefinition getEvent(String id) { return events.get(id); }
}
