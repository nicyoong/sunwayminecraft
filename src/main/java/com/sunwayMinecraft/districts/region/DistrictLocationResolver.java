package com.sunwayMinecraft.districts.region;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import org.bukkit.Location;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves districts at a location across both shape types. When districts
 * overlap, the effective district is chosen by priority:
 * archived first, then sanctuary, then the smaller (more specific) shape;
 * ties fall back to config order with a warning logged.
 */
public class DistrictLocationResolver {
    private static final Logger LOGGER = Logger.getLogger(DistrictLocationResolver.class.getName());
    private final DistrictsConfigManager configManager;

    public DistrictLocationResolver(DistrictsConfigManager configManager) {
        this.configManager = configManager;
    }

    /** Every enabled district whose shape contains the location. */
    public List<DistrictDefinition> getDistrictsAt(Location location) {
        List<DistrictDefinition> matches = new ArrayList<>();
        if (location == null) {
            return matches;
        }
        for (DistrictDefinition district : configManager.getDistricts()) {
            if (district.isEnabled() && district.getShape().contains(location)) {
                matches.add(district);
            }
        }
        return matches;
    }

    /**
     * The effective district at a location after overlap priority:
     * archived, then sanctuary, then smaller volume, then config order.
     */
    public DistrictDefinition getDistrictAt(Location location) {
        List<DistrictDefinition> matches = getDistrictsAt(location);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        matches.sort(Comparator
                .comparingInt((DistrictDefinition d) -> typePriority(d.getDistrictType()))
                .thenComparingLong(d -> d.getShape().getVolume()));
        if (typePriority(matches.get(0).getDistrictType())
                == typePriority(matches.get(1).getDistrictType())) {
            // same priority tier and ordering already applied; report the ambiguity
            LOGGER.warning("[Districts] Overlapping districts at the same priority: '"
                    + matches.get(0).getId() + "' wins over '" + matches.get(1).getId()
                    + "' (config order decides).");
        }
        return matches.get(0);
    }

    /** Pure containment check against a single district's shape. */
    public boolean isInsideDistrict(Location location, DistrictDefinition district) {
        return district != null && district.getShape().contains(location);
    }

    private static int typePriority(DistrictType type) {
        if (type == DistrictType.ARCHIVED) return 0;
        if (type == DistrictType.SANCTUARY) return 1;
        return 2;
    }
}
