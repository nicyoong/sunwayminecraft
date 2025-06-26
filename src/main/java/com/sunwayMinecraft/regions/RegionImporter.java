package com.sunwayMinecraft.regions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RegionImporter {
    public void importLegacyRegions(JavaPlugin plugin, RegionManager regionManager) {
        File legacyFile = new File(plugin.getDataFolder(), "lightregions.yml");
        if (!legacyFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        if (regionsSection == null) return;

        int imported = 0;
        for (String name : regionsSection.getKeys(false)) {
            String path = "regions." + name + ".";
            String worldName = config.getString(path + "world");
            int minX = config.getInt(path + "min.x");
            int minY = config.getInt(path + "min.y");
            int minZ = config.getInt(path + "min.z");
            int maxX = config.getInt(path + "max.x");
            int maxY = config.getInt(path + "max.y");
            int maxZ = config.getInt(path + "max.z");

            if (regionManager.createRegion(name, worldName, minX, minY, minZ,
                    maxX, maxY, maxZ, null, false)) {
                imported++;
            }
        }
        plugin.getLogger().info("Imported " + imported + " regions from legacy configuration");
    }
}