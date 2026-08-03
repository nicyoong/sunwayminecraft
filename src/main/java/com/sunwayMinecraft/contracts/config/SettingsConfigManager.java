package com.sunwayMinecraft.contracts.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SettingsConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    
    private int maxActiveContracts = 3;
    private boolean remoteAcceptEnabled = false;

    public SettingsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contract-settings.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contract-settings.yml", false);
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        maxActiveContracts = config.getInt("max_active_contracts", 3);
        remoteAcceptEnabled = config.getBoolean("remote_accept_enabled", false);
    }

    public int getMaxActiveContracts() { return maxActiveContracts; }
    public boolean isRemoteAcceptEnabled() { return remoteAcceptEnabled; }
}
