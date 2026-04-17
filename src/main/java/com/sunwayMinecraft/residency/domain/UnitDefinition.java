package com.sunwayMinecraft.residency.domain;

import com.sunwayMinecraft.residency.region.Region3i;
import java.util.Collections;
import java.util.Map;

public class UnitDefinition {
    private final String id;
    private final String buildingId;
    private final String districtId;
    private final String displayName;
    private final String unitCode;
    private final UnitType unitType;
    private final UnitMode mode;
    private final int prestigeTier;
    private final String addressLine;
    private final String floorLabel;
    private final String pricingProfileId;
    private final String policyProfileId;
    private final int capacity;
    private final UnitFlags flags;
    private final ListingSettings listingSettings;
    private final Region3i primaryRegion;
    private final Map<String, Region3i> linkedRegions;

    public UnitDefinition(String id, String buildingId, String districtId, String displayName, String unitCode,
                          UnitType unitType, UnitMode mode, int prestigeTier, String addressLine, String floorLabel,
                          String pricingProfileId, String policyProfileId, int capacity, UnitFlags flags,
                          ListingSettings listingSettings, Region3i primaryRegion, Map<String, Region3i> linkedRegions) {
        this.id = id;
        this.buildingId = buildingId;
        this.districtId = districtId;
        this.displayName = displayName;
        this.unitCode = unitCode;
        this.unitType = unitType;
        this.mode = mode;
        this.prestigeTier = prestigeTier;
        this.addressLine = addressLine;
        this.floorLabel = floorLabel;
        this.pricingProfileId = pricingProfileId;
        this.policyProfileId = policyProfileId;
        this.capacity = capacity;
        this.flags = flags;
        this.listingSettings = listingSettings;
        this.primaryRegion = primaryRegion;
        this.linkedRegions = linkedRegions == null ? Collections.emptyMap() : Collections.unmodifiableMap(linkedRegions);
    }

    public String getId() { return id; }
    public String getBuildingId() { return buildingId; }
    public String getDistrictId() { return districtId; }
    public String getDisplayName() { return displayName; }
    public String getUnitCode() { return unitCode; }
    public UnitType getUnitType() { return unitType; }
    public UnitMode getMode() { return mode; }
    public int getPrestigeTier() { return prestigeTier; }
    public String getAddressLine() { return addressLine; }
    public String getFloorLabel() { return floorLabel; }
    public String getPricingProfileId() { return pricingProfileId; }
    public String getPolicyProfileId() { return policyProfileId; }
    public int getCapacity() { return capacity; }
    public UnitFlags getFlags() { return flags; }
    public ListingSettings getListingSettings() { return listingSettings; }
    public Region3i getPrimaryRegion() { return primaryRegion; }
    public Map<String, Region3i> getLinkedRegions() { return linkedRegions; }
}
