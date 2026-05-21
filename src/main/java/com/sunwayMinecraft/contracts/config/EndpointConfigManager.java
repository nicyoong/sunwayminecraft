package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EndpointConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    private final Map<String, ContractEndpoint> endpoints = new HashMap<>();

    public EndpointConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contract-endpoints.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contract-endpoints.yml", false);
        }
    }

    public void load() {
        endpoints.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("endpoints");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                String name = section.getString(key + ".name");
                ContractEndpoint.EndpointType type = ContractEndpoint.EndpointType.valueOf(section.getString(key + ".type"));
                String worldName = section.getString(key + ".world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                double radius = section.getDouble(key + ".radius", 3.0);

                if (Bukkit.getWorld(worldName) == null) {
                    plugin.getLogger().warning("World not found for endpoint " + key + ": " + worldName);
                }

                endpoints.put(key, new ContractEndpoint(
                    key, name, type, new Location(Bukkit.getWorld(worldName), x, y, z), radius
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load endpoint: " + key, e);
            }
        }
    }

    public Map<String, ContractEndpoint> getEndpoints() { return endpoints; }
    public ContractEndpoint getEndpoint(String id) { return endpoints.get(id); }
}
