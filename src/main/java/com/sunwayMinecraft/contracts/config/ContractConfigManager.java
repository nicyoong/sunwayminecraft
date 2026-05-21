package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ContractConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    private final Map<String, ContractDefinition> contracts = new HashMap<>();

    public ContractConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contracts.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contracts.yml", false);
        }
        load();
    }

    public void load() {
        contracts.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("contracts");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                ContractCategory category = ContractCategory.valueOf(section.getString(key + ".category"));
                String name = section.getString(key + ".name");
                String description = section.getString(key + ".description");
                double reward = section.getDouble(key + ".reward_money");
                long duration = section.getLong(key + ".duration_minutes", 60);
                long cooldown = section.getLong(key + ".cooldown_minutes", 30);
                String startEndpoint = section.getString(key + ".start_endpoint");
                String endEndpoint = section.getString(key + ".end_endpoint");
                String objective = section.getString(key + ".objective_description");

                Map<Material, Integer> materials = new HashMap<>();
                ConfigurationSection matSection = section.getConfigurationSection(key + ".required_materials");
                if (matSection != null) {
                    for (String matKey : matSection.getKeys(false)) {
                        Material material = Material.matchMaterial(matKey);
                        if (material != null) {
                            materials.put(material, matSection.getInt(matKey));
                        }
                    }
                }

                contracts.put(key, new ContractDefinition(
                    key, category, name, description, reward, duration, cooldown,
                    startEndpoint, endEndpoint, materials, objective
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load contract: " + key, e);
            }
        }
    }

    public Map<String, ContractDefinition> getContracts() { return contracts; }
    public ContractDefinition getContract(String id) { return contracts.get(id); }
}
