package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.*;
        import com.sunwayMinecraft.residency.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitsConfigManager extends AbstractYamlConfigManager {
    private final Map<String, UnitDefinition> units = new LinkedHashMap<>();

    public UnitsConfigManager(JavaPlugin plugin) { super(plugin, "units.yml"); }

    @Override
    protected void load() {
        units.clear();
        ConfigurationSection root = config.getConfigurationSection("units");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            String district = s.getString("district");
            String building = s.getString("building");
            String displayName = s.getString("display-name", id);
            String unitCode = s.getString("unit-code", id);
            UnitType unitType = UnitType.valueOf(s.getString("unit-type", "APARTMENT").toUpperCase());
            UnitMode mode = UnitMode.valueOf(s.getString("mode", "RESIDENTIAL").toUpperCase());
            int prestigeTier = s.getInt("prestige-tier", 1);
            String addressLine = s.getString("address-line", displayName);
            String floorLabel = s.getString("floor-label", "");
            String pricingProfile = s.getString("pricing-profile", "default");
            String policyProfile = s.getString("policy-profile", "default");
            int capacity = s.getInt("capacity", 1);
            String world = s.getString("world", "world");
            ConfigurationSection flags = s.getConfigurationSection("flags");
            UnitFlags unitFlags = new UnitFlags(
                    flags != null && flags.getBoolean("public-entry", false),
                    flags == null || flags.getBoolean("allow-guests", true),
                    flags != null && flags.getBoolean("allow-event-access", true),
                    flags == null || flags.getBoolean("escrow-on-repossession", true),
                    flags != null && flags.getBoolean("public-interaction", false),
                    flags != null && flags.getBoolean("public-container-access", false)
            );
            ConfigurationSection listing = s.getConfigurationSection("listing");
            ListingSettings listingSettings = new ListingSettings(
                    listing == null || listing.getBoolean("visible", true),
                    listing != null && listing.getBoolean("approval-required", false),
                    listing == null ? List.of() : listing.getStringList("tags")
            );
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
            units.put(id.toLowerCase(), new UnitDefinition(id, building, district, displayName, unitCode, unitType, mode,
                    prestigeTier, addressLine, floorLabel, pricingProfile, policyProfile, capacity, unitFlags,
                    listingSettings, primary, linked));
        }
    }

    public UnitDefinition getUnit(String id) { return units.get(id.toLowerCase()); }
    public Collection<UnitDefinition> getUnits() { return units.values(); }
}
