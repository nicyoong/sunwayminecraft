package com.sunwayMinecraft.residency.domain;

import com.sunwayMinecraft.residency.region.Region3i;

public class DistrictDefinition {
    private final String id;
    private final String displayName;
    private final String world;
    private final boolean enabled;
    private final int prestigeTier;
    private final String pricingProfileId;
    private final String policyProfileId;
    private final String description;
    private final Region3i region;

    public DistrictDefinition(String id, String displayName, String world, boolean enabled, int prestigeTier,
                              String pricingProfileId, String policyProfileId, String description, Region3i region) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.enabled = enabled;
        this.prestigeTier = prestigeTier;
        this.pricingProfileId = pricingProfileId;
        this.policyProfileId = policyProfileId;
        this.description = description;
        this.region = region;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getWorld() { return world; }
    public boolean isEnabled() { return enabled; }
    public int getPrestigeTier() { return prestigeTier; }
    public String getPricingProfileId() { return pricingProfileId; }
    public String getPolicyProfileId() { return policyProfileId; }
    public String getDescription() { return description; }
    public Region3i getRegion() { return region; }
}
