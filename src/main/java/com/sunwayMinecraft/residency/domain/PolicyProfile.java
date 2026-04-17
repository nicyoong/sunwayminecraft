package com.sunwayMinecraft.residency.domain;

public class PolicyProfile {
    private final String id;
    private final UnitMode defaultRolePreset;
    private final int rentGraceDays;
    private final boolean repossessionEnabled;
    private final boolean autoRestrictOnArrears;
    private final boolean allowPublicContainerAccess;
    private final boolean allowPublicInteraction;

    public PolicyProfile(String id, UnitMode defaultRolePreset, int rentGraceDays, boolean repossessionEnabled,
                         boolean autoRestrictOnArrears, boolean allowPublicContainerAccess, boolean allowPublicInteraction) {
        this.id = id;
        this.defaultRolePreset = defaultRolePreset;
        this.rentGraceDays = rentGraceDays;
        this.repossessionEnabled = repossessionEnabled;
        this.autoRestrictOnArrears = autoRestrictOnArrears;
        this.allowPublicContainerAccess = allowPublicContainerAccess;
        this.allowPublicInteraction = allowPublicInteraction;
    }

    public String getId() { return id; }
    public UnitMode getDefaultRolePreset() { return defaultRolePreset; }
    public int getRentGraceDays() { return rentGraceDays; }
    public boolean isRepossessionEnabled() { return repossessionEnabled; }
    public boolean isAutoRestrictOnArrears() { return autoRestrictOnArrears; }
    public boolean isAllowPublicContainerAccess() { return allowPublicContainerAccess; }
    public boolean isAllowPublicInteraction() { return allowPublicInteraction; }
}
