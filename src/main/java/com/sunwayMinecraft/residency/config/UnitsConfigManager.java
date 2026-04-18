package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.*;
import com.sunwayMinecraft.residency.region.Region3i;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitsConfigManager extends AbstractYamlConfigManager {
    private final Map<String, UnitDefinition> units = new LinkedHashMap<>();

    public UnitsConfigManager(JavaPlugin plugin) {
        super(plugin, "units.yml");
    }

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

            String world = s.getString("world", "world");
            Region3i primary = readRegion(world, s.getConfigurationSection("primary-region"));
            Map<String, Region3i> linked = readLinked(world, s.getConfigurationSection("linked-regions"));

            units.put(id.toLowerCase(), new UnitDefinition(
                    id,
                    building,
                    district,
                    displayName,
                    unitCode,
                    unitType,
                    mode,
                    prestigeTier,
                    addressLine,
                    floorLabel,
                    pricingProfile,
                    policyProfile,
                    capacity,
                    unitFlags,
                    listingSettings,
                    primary,
                    linked
            ));
        }
    }

    private Region3i readRegion(String world, ConfigurationSection section) {
        if (section == null) return null;
        return new Region3i(
                world,
                section.getConfigurationSection("min").getInt("x"),
                section.getConfigurationSection("min").getInt("y"),
                section.getConfigurationSection("min").getInt("z"),
                section.getConfigurationSection("max").getInt("x"),
                section.getConfigurationSection("max").getInt("y"),
                section.getConfigurationSection("max").getInt("z")
        );
    }

    private Map<String, Region3i> readLinked(String world, ConfigurationSection section) {
        Map<String, Region3i> out = new LinkedHashMap<>();
        if (section == null) return out;
        for (String key : section.getKeys(false)) {
            out.put(key, readRegion(world, section.getConfigurationSection(key)));
        }
        return out;
    }

    public UnitDefinition getUnit(String id) {
        return units.get(id.toLowerCase());
    }

    public Collection<UnitDefinition> getUnits() {
        return units.values();
    }

    public void saveNewUnitFromSelection(String id,
                                         String district,
                                         String building,
                                         UnitMode mode,
                                         UnitType type,
                                         String pricingProfile,
                                         String policyProfile,
                                         Location pos1,
                                         Location pos2) {
        String base = "units." + id + ".";
        config.set(base + "district", district);
        config.set(base + "building", building);
        config.set(base + "display-name", id);
        config.set(base + "unit-code", id);
        config.set(base + "unit-type", type.name());
        config.set(base + "mode", mode.name());
        config.set(base + "prestige-tier", 1);
        config.set(base + "address-line", id);
        config.set(base + "floor-label", "");
        config.set(base + "pricing-profile", pricingProfile);
        config.set(base + "policy-profile", policyProfile);
        config.set(base + "capacity", 1);

        config.set(base + "flags.public-entry", mode == UnitMode.COMMERCIAL || mode == UnitMode.MIXED_USE);
        config.set(base + "flags.allow-guests", true);
        config.set(base + "flags.allow-event-access", true);
        config.set(base + "flags.escrow-on-repossession", true);
        config.set(base + "flags.public-interaction", mode == UnitMode.COMMERCIAL || mode == UnitMode.MIXED_USE);
        config.set(base + "flags.public-container-access", false);

        config.set(base + "listing.visible", true);
        config.set(base + "listing.approval-required", false);
        config.set(base + "listing.tags", List.of(mode.name().toLowerCase(), type.name().toLowerCase()));

        config.set(base + "world", pos1.getWorld().getName());
        saveRegion(base + "primary-region", pos1, pos2);

        save();
        reload();
    }

    public void saveLinkedSubregion(String unitId, String name, Location pos1, Location pos2) {
        String base = "units." + unitId + ".linked-regions." + name;
        saveRegion(base, pos1, pos2);
        save();
        reload();
    }

    private void saveRegion(String path, Location pos1, Location pos2) {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        config.set(path + ".min.x", minX);
        config.set(path + ".min.y", minY);
        config.set(path + ".min.z", minZ);
        config.set(path + ".max.x", maxX);
        config.set(path + ".max.y", maxY);
        config.set(path + ".max.z", maxZ);
    }
}
