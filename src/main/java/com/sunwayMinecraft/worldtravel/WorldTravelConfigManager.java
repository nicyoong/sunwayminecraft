package com.sunwayMinecraft.worldtravel;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldTravelConfigManager {

    private static final String FILE_NAME = "worldtravel.yml";
    private static final String STATE_PATH = "mining-world.state";

    private final JavaPlugin plugin;
    private final File configFile;

    public WorldTravelConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), FILE_NAME);
        ensureFileExists();
    }

    public MiningWorldState loadMiningWorldState() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String rawState = config.getString(STATE_PATH, MiningWorldState.OPEN.name());

        try {
            return MiningWorldState.valueOf(rawState.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning(
                    "Invalid mining world state '" + rawState + "' in " + FILE_NAME + ". Falling back to OPEN.");
            return MiningWorldState.OPEN;
        }
    }

    public void saveMiningWorldState(MiningWorldState state) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set(STATE_PATH, state.name());
        saveConfig(config);
    }

    private void ensureFileExists() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for world travel config.");
        }

        if (!configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set(STATE_PATH, MiningWorldState.OPEN.name());
            saveConfig(config);
        }
    }

    private void saveConfig(FileConfiguration config) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}