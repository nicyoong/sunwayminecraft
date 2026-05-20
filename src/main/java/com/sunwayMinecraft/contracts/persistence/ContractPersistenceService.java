package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class ContractPersistenceService {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Map<UUID, List<ActiveContract>> activeContracts = new HashMap<>();
    private final Map<UUID, Map<String, Instant>> cooldowns = new HashMap<>();

    public ContractPersistenceService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "contracts-data.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        // Load Active Contracts
        ConfigurationSection activeSection = config.getConfigurationSection("active");
        if (activeSection != null) {
            for (String uuidStr : activeSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                List<ActiveContract> list = new ArrayList<>();
                ConfigurationSection playerSection = activeSection.getConfigurationSection(uuidStr);
                if (playerSection != null) {
                    for (String key : playerSection.getKeys(false)) {
                        String contractId = playerSection.getString(key + ".id");
                        Instant start = Instant.parse(playerSection.getString(key + ".start"));
                        Instant expiry = Instant.parse(playerSection.getString(key + ".expiry"));
                        ActiveContract ac = new ActiveContract(uuid, contractId, start, expiry);
                        ac.setProgress(playerSection.getDouble(key + ".progress"));
                        list.add(ac);
                    }
                }
                activeContracts.put(uuid, list);
            }
        }

        // Load Cooldowns
        ConfigurationSection cooldownSection = config.getConfigurationSection("cooldowns");
        if (cooldownSection != null) {
            for (String uuidStr : cooldownSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Instant> map = new HashMap<>();
                ConfigurationSection playerSection = cooldownSection.getConfigurationSection(uuidStr);
                if (playerSection != null) {
                    for (String contractId : playerSection.getKeys(false)) {
                        map.put(contractId, Instant.parse(playerSection.getString(contractId)));
                    }
                }
                cooldowns.put(uuid, map);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();

        // Save Active Contracts
        for (Map.Entry<UUID, List<ActiveContract>> entry : activeContracts.entrySet()) {
            int i = 0;
            for (ActiveContract ac : entry.getValue()) {
                String path = "active." + entry.getKey() + "." + i++;
                config.set(path + ".id", ac.getContractId());
                config.set(path + ".start", ac.getStartTime().toString());
                config.set(path + ".expiry", ac.getExpiryTime().toString());
                config.set(path + ".progress", ac.getProgress());
            }
        }

        // Save Cooldowns
        for (Map.Entry<UUID, Map<String, Instant>> entry : cooldowns.entrySet()) {
            for (Map.Entry<String, Instant> cooldownEntry : entry.getValue().entrySet()) {
                config.set("cooldowns." + entry.getKey() + "." + cooldownEntry.getKey(), cooldownEntry.getValue().toString());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save contract data!", e);
        }
    }

    public List<ActiveContract> getPlayerContracts(UUID uuid) {
        return activeContracts.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    public Map<String, Instant> getPlayerCooldowns(UUID uuid) {
        return cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
    }
}
