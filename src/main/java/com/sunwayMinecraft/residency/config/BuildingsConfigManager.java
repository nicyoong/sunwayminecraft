package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.BuildingDefinition;
import com.sunwayMinecraft.residency.domain.BuildingType;
import com.sunwayMinecraft.residency.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BuildingsConfigManager extends AbstractYamlConfigManager {
    private final Map<String, BuildingDefinition> buildings = new LinkedHashMap<>();

    public BuildingsConfigManager(JavaPlugin plugin) { super(plugin, "buildings.yml"); }

    @Override
    protected void load() {
        buildings.clear();
        ConfigurationSection root = config.getConfigurationSection("buildings");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            String district = s.getString("district");
            String displayName = s.getString("display-name", id);
            String shortCode = s.getString("short-code", id);
            BuildingType type = BuildingType.valueOf(s.getString("building-type", "STANDALONE").toUpperCase());
            int prestigeTier = s.getInt("prestige-tier", 1);
            String addressBase = s.getString("address-base", displayName);
            String world = s.getString("world", "world");
            String listingVisibility = s.getString("listing-visibility", "PUBLIC");
            Region3i primary = new Region3i(world,
                    s.getConfigurationSection("primary-region").getConfigurationSection("min").getInt("x"),
                    s.getConfigurationSection("primary-region").getConfigurationSection("min").getInt("y"),
                    s.getConfigurationSection("primary-region").getConfigurationSection("min").getInt("z"),
                    s.getConfigurationSection("primary-region").getConfigurationSection("max").getInt("x"),
                    s.getConfigurationSection("primary-region").getConfigurationSection("max").getInt("y"),
                    s.getConfigurationSection("primary-region").getConfigurationSection("max").getInt("z"));
            Map<String, Region3i> linked = new LinkedHashMap<>();
            ConfigurationSection linkedRoot = s.getConfigurationSection("linked-regions");
            if (linkedRoot != null) {
                for (String key : linkedRoot.getKeys(false)) {
                    ConfigurationSection r = linkedRoot.getConfigurationSection(key);
                    linked.put(key, new Region3i(world,
                            r.getConfigurationSection("min").getInt("x"),
                            r.getConfigurationSection("min").getInt("y"),
                            r.getConfigurationSection("min").getInt("z"),
                            r.getConfigurationSection("max").getInt("x"),
                            r.getConfigurationSection("max").getInt("y"),
                            r.getConfigurationSection("max").getInt("z")));
                }
            }
            buildings.put(id.toLowerCase(), new BuildingDefinition(id, district, displayName, shortCode, type, prestigeTier, addressBase, world, listingVisibility, primary, linked));
        }
    }

    public BuildingDefinition getBuilding(String id) { return buildings.get(id.toLowerCase()); }
    public Collection<BuildingDefinition> getBuildings() { return buildings.values(); }
}
