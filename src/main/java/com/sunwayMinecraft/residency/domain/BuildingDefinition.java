package com.sunwayMinecraft.residency.domain;

import com.sunwayMinecraft.residency.region.Region3i;
import java.util.Collections;
import java.util.Map;

public class BuildingDefinition {
    private final String id;
    private final String districtId;
    private final String displayName;
    private final String shortCode;
    private final BuildingType buildingType;
    private final int prestigeTier;
    private final String addressBase;
    private final String world;
    private final String listingVisibility;
    private final Region3i primaryRegion;
    private final Map<String, Region3i> linkedRegions;

    public BuildingDefinition(String id, String districtId, String displayName, String shortCode, BuildingType buildingType,
                              int prestigeTier, String addressBase, String world, String listingVisibility,
                              Region3i primaryRegion, Map<String, Region3i> linkedRegions) {
        this.id = id;
        this.districtId = districtId;
        this.displayName = displayName;
        this.shortCode = shortCode;
        this.buildingType = buildingType;
        this.prestigeTier = prestigeTier;
        this.addressBase = addressBase;
        this.world = world;
        this.listingVisibility = listingVisibility;
        this.primaryRegion = primaryRegion;
        this.linkedRegions = linkedRegions == null ? Collections.emptyMap() : Collections.unmodifiableMap(linkedRegions);
    }

    public String getId() { return id; }
    public String getDistrictId() { return districtId; }
    public String getDisplayName() { return displayName; }
    public String getShortCode() { return shortCode; }
    public BuildingType getBuildingType() { return buildingType; }
    public int getPrestigeTier() { return prestigeTier; }
    public String getAddressBase() { return addressBase; }
    public String getWorld() { return world; }
    public String getListingVisibility() { return listingVisibility; }
    public Region3i getPrimaryRegion() { return primaryRegion; }
    public Map<String, Region3i> getLinkedRegions() { return linkedRegions; }
}
