package com.sunwayMinecraft.city.metrics;

import org.bukkit.plugin.java.JavaPlugin;

public class CityMetricsManager {
    private final CityMetricsRepository repository;

    public CityMetricsManager(JavaPlugin plugin) {
        this.repository = new CityMetricsRepository(plugin);
    }

    public void initialize() {
        repository.load();
    }

    public void save() {
        repository.save();
    }

    public void increment(String key) {
        increment(key, 1.0);
    }

    public void increment(String key, double amount) {
        repository.increment(key, amount);
    }

    public CityMetricSnapshot getSnapshot() {
        return new CityMetricSnapshot(repository.getAll());
    }
}
