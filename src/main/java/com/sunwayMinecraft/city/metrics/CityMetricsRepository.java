package com.sunwayMinecraft.city.metrics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CityMetricsRepository {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Double> metrics = new ConcurrentHashMap<>();

    public CityMetricsRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "city-metrics.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(true)) {
            if (config.isDouble(key) || config.isInt(key)) {
                metrics.put(key, config.getDouble(key));
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        metrics.forEach(config::set);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save city-metrics.yml", e);
        }
    }

    public void increment(String key, double amount) {
        metrics.merge(key, amount, Double::sum);
    }

    public double get(String key) {
        return metrics.getOrDefault(key, 0.0);
    }

    public Map<String, Double> getAll() {
        return new HashMap<>(metrics);
    }
}
