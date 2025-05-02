package com.sunwayMinecraft.switches;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import java.io.File;
import java.util.*;

public class LightConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration lightConfig;
    private File configFile;

    public LightConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "lightregions.yml");
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("lightregions.yml", false);
        }
        lightConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public Map<String, LightRegion> getRegions() {
        Map<String, LightRegion> regions = new HashMap<>();
        ConfigurationSection regionsSection = lightConfig.getConfigurationSection("regions");

        if (regionsSection == null) return regions;

        for (String regionName : regionsSection.getKeys(false)) {
            String path = "regions." + regionName + ".";
            World world = plugin.getServer().getWorld(lightConfig.getString(path + "world"));

            regions.put(regionName, new LightRegion(
                    regionName,
                    world,
                    lightConfig.getInt(path + "min.x"),
                    lightConfig.getInt(path + "min.y"),
                    lightConfig.getInt(path + "min.z"),
                    lightConfig.getInt(path + "max.x"),
                    lightConfig.getInt(path + "max.y"),
                    lightConfig.getInt(path + "max.z")
            ));
        }
        return regions;
    }
}
