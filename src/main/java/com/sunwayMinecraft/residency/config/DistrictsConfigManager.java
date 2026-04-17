package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.DistrictDefinition;
import com.sunwayMinecraft.residency.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DistrictsConfigManager extends AbstractYamlConfigManager {
    private final Map<String, DistrictDefinition> districts = new LinkedHashMap<>();

    public DistrictsConfigManager(JavaPlugin plugin) {
        super(plugin, "districts.yml");
    }

    @Override
    protected void load() {
        districts.clear();
        ConfigurationSection root = config.getConfigurationSection("districts");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            String displayName = section.getString("display-name", id);
            String world = section.getString("world", "world");
            boolean enabled = section.getBoolean("enabled", true);
            int prestigeTier = section.getInt("prestige-tier", 1);
            String pricingProfile = section.getString("pricing-profile", "default");
            String policyProfile = section.getString("policy-profile", "default");
            String description = section.getString("description", "");
            ConfigurationSection regionSec = section.getConfigurationSection("region");
            if (regionSec == null) continue;
            Region3i region = new Region3i(world,
                    regionSec.getConfigurationSection("min").getInt("x"),
                    regionSec.getConfigurationSection("min").getInt("y"),
                    regionSec.getConfigurationSection("min").getInt("z"),
                    regionSec.getConfigurationSection("max").getInt("x"),
                    regionSec.getConfigurationSection("max").getInt("y"),
                    regionSec.getConfigurationSection("max").getInt("z"));
            districts.put(id.toLowerCase(), new DistrictDefinition(id, displayName, world, enabled, prestigeTier, pricingProfile, policyProfile, description, region));
        }
    }

    public DistrictDefinition getDistrict(String id) { return districts.get(id.toLowerCase()); }
    public Collection<DistrictDefinition> getDistricts() { return districts.values(); }
}
