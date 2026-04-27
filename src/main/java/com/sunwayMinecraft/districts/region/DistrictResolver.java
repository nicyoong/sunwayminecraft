package com.sunwayMinecraft.districts.region;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import org.bukkit.Location;

public class DistrictResolver {
    private final DistrictsConfigManager configManager;

    public DistrictResolver(DistrictsConfigManager configManager) {
        this.configManager = configManager;
    }

    public DistrictDefinition resolve(Location location) {
        if (location == null) return null;
        for (DistrictDefinition district : configManager.getDistricts()) {
            if (!district.isEnabled()) continue;
            if (district.getRegion().contains(location)) {
                return district;
            }
        }
        return null;
    }
}
