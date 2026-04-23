package com.sunwayMinecraft.districts.domain;

import com.sunwayMinecraft.districts.region.Region3i;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DistrictDefinition {
    private final String id;
    private final String displayName;
    private final String shortName;
    private final String world;
    private final Region3i region;
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

    public DistrictDefinition(String id,
                              String displayName,
                              String shortName,
                              String world,
                              Region3i region,
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
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.shortName = shortName;
        this.world = Objects.requireNonNull(world, "world");
        this.region = Objects.requireNonNull(region, "region");
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
    }

