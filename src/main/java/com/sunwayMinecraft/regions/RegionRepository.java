package com.sunwayMinecraft.regions;

import java.util.*;
import org.bukkit.Location;

public class RegionRepository {
    private final Map<String, Region> regionsByName = new HashMap<>();
    private final List<Region> spatialIndex = new ArrayList<>();
    private final Map<Long, Region> regionsByClaimId = new HashMap<>();

    public void addRegion(Region region) {
        regionsByName.put(region.getName().toLowerCase(), region);
        spatialIndex.add(region);

        if (region.getClaimId() != null) {
            regionsByClaimId.put(region.getClaimId(), region);
        }
    }

    public void removeRegion(Region region) {
        regionsByName.remove(region.getName().toLowerCase());
        spatialIndex.remove(region);

        if (region.getClaimId() != null) {
            regionsByClaimId.remove(region.getClaimId());
        }
    }

    public Region getByName(String name) {
        return regionsByName.get(name.toLowerCase());
    }

    public Region getByClaimId(long claimId) {
        return regionsByClaimId.get(claimId);
    }

    public List<Region> getRegionsAt(Location location) {
        List<Region> result = new ArrayList<>();
        for (Region region : spatialIndex) {
            if (region.contains(location)) {
                result.add(region);
            }
        }
        return result;
    }

    public Map<String, Region> getAllRegions() {
        return Collections.unmodifiableMap(regionsByName);
    }
}