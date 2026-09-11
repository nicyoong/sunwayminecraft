package com.sunwayMinecraft.districts.domain;

import com.sunwayMinecraft.districts.region.DistrictShape;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DistrictDefinition {
    private final String id;
    private final String displayName;
    private final String shortName;
    private final String world;
    private final DistrictShape shape;
    private final boolean enabled;
    private final DistrictType districtType;
    private final int prestigeTier;
    private final String publicSummary;
    private final List<String> tags;
    private final boolean publicVisible;
    private final int listingPriority;
    private final boolean storefrontPriority;
    private final boolean residencyPriority;
    private final ApprovalBias recommendedApprovalBias;
    private final boolean allowPublicEvents;
    private final boolean signatureArea;
    private final DistrictOwnership ownership;

    public DistrictDefinition(String id,
                              String displayName,
                              String shortName,
                              String world,
                              com.sunwayMinecraft.districts.region.Region3i region,
                              boolean enabled,
                              DistrictType districtType,
                              int prestigeTier,
                              String publicSummary,
                              List<String> tags,
                              boolean publicVisible,
                              int listingPriority,
                              boolean storefrontPriority,
                              boolean residencyPriority,
                              ApprovalBias recommendedApprovalBias,
                              boolean allowPublicEvents,
                              boolean signatureArea) {
        this(id, displayName, shortName, world, DistrictShape.cuboid(region), enabled,
                districtType, prestigeTier,
                publicSummary, tags, publicVisible, listingPriority, storefrontPriority,
                residencyPriority, recommendedApprovalBias, allowPublicEvents, signatureArea,
                DistrictOwnership.neutral());
    }

    public DistrictDefinition(String id,
                              String displayName,
                              String shortName,
                              String world,
                              DistrictShape shape,
                              boolean enabled,
                              DistrictType districtType,
                              int prestigeTier,
                              String publicSummary,
                              List<String> tags,
                              boolean publicVisible,
                              int listingPriority,
                              boolean storefrontPriority,
                              boolean residencyPriority,
                              ApprovalBias recommendedApprovalBias,
                              boolean allowPublicEvents,
                              boolean signatureArea,
                              DistrictOwnership ownership) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.shortName = shortName;
        this.world = Objects.requireNonNull(world, "world");
        this.shape = Objects.requireNonNull(shape, "shape");
        this.enabled = enabled;
        this.districtType = Objects.requireNonNull(districtType, "districtType");
        this.prestigeTier = prestigeTier;
        this.publicSummary = publicSummary == null ? "" : publicSummary;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.publicVisible = publicVisible;
        this.listingPriority = listingPriority;
        this.storefrontPriority = storefrontPriority;
        this.residencyPriority = residencyPriority;
        this.recommendedApprovalBias = recommendedApprovalBias == null ? ApprovalBias.STANDARD : recommendedApprovalBias;
        this.allowPublicEvents = allowPublicEvents;
        this.signatureArea = signatureArea;
        this.ownership = ownership == null ? DistrictOwnership.neutral() : ownership;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getShortName() { return shortName; }
    public String getWorld() { return world; }
    public String getWorldName() { return world; }
    public DistrictShape getShape() { return shape; }
    public boolean isEnabled() { return enabled; }
    public DistrictType getDistrictType() { return districtType; }
    public int getPrestigeTier() { return prestigeTier; }
    public String getPublicSummary() { return publicSummary; }
    public List<String> getTags() { return Collections.unmodifiableList(tags); }
    public boolean isPublicVisible() { return publicVisible; }
    public int getListingPriority() { return listingPriority; }
    public boolean isStorefrontPriority() { return storefrontPriority; }
    public boolean isResidencyPriority() { return residencyPriority; }
    public ApprovalBias getRecommendedApprovalBias() { return recommendedApprovalBias; }
    public boolean isAllowPublicEvents() { return allowPublicEvents; }
    public boolean isSignatureArea() { return signatureArea; }
    public DistrictOwnership getOwnership() { return ownership; }

    /** Returns a copy of this definition with new ownership and/or type. */
    public DistrictDefinition getDefinitionWith(DistrictOwnership newOwnership, DistrictType newType) {
        return new DistrictDefinition(id, displayName, shortName, world, shape, enabled, newType,
                prestigeTier, publicSummary, tags, publicVisible, listingPriority,
                storefrontPriority, residencyPriority, recommendedApprovalBias,
                allowPublicEvents, signatureArea,
                newOwnership == null ? DistrictOwnership.neutral() : newOwnership);
    }

    public DistrictAccessRule getAccessRule() {
        return DistrictAccessRule.forOwnership(ownership);
    }

    /** True when this district's type forbids building, renting and trading. */
    public boolean isArchived() {
        return districtType == DistrictType.ARCHIVED;
    }

    public String getDisplayShortNameOrName() {
        return shortName != null && !shortName.isBlank() ? shortName : displayName;
    }
}
