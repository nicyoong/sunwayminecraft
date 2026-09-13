package com.sunwayMinecraft.contracts.domain;

import org.bukkit.Location;

public record ContractEndpoint(
    String id,
    String name,
    EndpointType type,
    Location location,
    double radius,
    String campus,
    String districtId,
    String alignmentOwner,
    boolean enabled
) {
    public enum EndpointType {
        BOARD,
        DEPOT,
        PICKUP,
        DROPOFF,
        MAINTENANCE_POINT,
        SURVEY_POINT,
        SALVAGE_POINT,
        TASK_POINT,
        CAMPUS_GATE,
        TRANSIT_HUB
    }

    /** Compatibility constructor retaining the pre-Triple-Alliance shape. */
    public ContractEndpoint(String id, String name, EndpointType type, Location location, double radius) {
        this(id, name, type, location, radius, null, null, null, true);
    }
}
