package com.sunwayMinecraft.beacon;

import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

class BeaconConfigurationLoader implements ConfigurationLoader {
    private final JavaPlugin plugin;

    public BeaconConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getTicksPerTransition() {
        return plugin.getConfig().getInt("ticksPerTransition", 20);
    }

    @Override
    public List<Material> getColorCycle() {
        List<String> colorNames = plugin.getConfig().getStringList("beaconColors");
        List<Material> materials = new ArrayList<>();

        for (String colorName : colorNames) {
            try {
                Material material = Material.valueOf(colorName.toUpperCase());
                if (material.toString().contains("STAINED_GLASS")) {
                    materials.add(material);
                } else {
                    plugin.getLogger().log(Level.WARNING, "Invalid glass color in config: " + colorName);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid material in config: " + colorName);
            }
        }

        if (materials.isEmpty()) {
            plugin.getLogger().log(Level.SEVERE, "No valid beacon colors found in config, using default MAGENTA_STAINED_GLASS.");
            materials.add(Material.MAGENTA_STAINED_GLASS); // Default color
        }

        return materials;
    }

    @Override
    public Map<Location, Integer> loadBeacons() {
        Map<Location, Integer> beaconColors = new HashMap<>();
        FileConfiguration beaconConfig = ConfigLoader.loadBeaconLocations(plugin);
        List<Map<?, ?>> beacons = beaconConfig.getMapList("beaconLocations");

        for (Map<?, ?> beaconData : beacons) {
            try {
                String worldName = (String) beaconData.get("world");
                double x = ((Number) beaconData.get("x")).doubleValue();
                double y = ((Number) beaconData.get("y")).doubleValue();
                double z = ((Number) beaconData.get("z")).doubleValue();
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                beaconColors.put(location, 0); // Start with the default color index
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Invalid beacon location in beaconlocations.yml: " + beaconData);
            }
        }
        return beaconColors;
    }
}