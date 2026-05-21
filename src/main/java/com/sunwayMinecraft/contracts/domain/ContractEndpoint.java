package com.sunwayMinecraft.contracts.domain;

import org.bukkit.Location;

public record ContractEndpoint(
    String id,
    String name,
    EndpointType type,
    Location location,
    double radius
) {
    public enum EndpointType {
        BOARD,
        DEPOT,
        PICKUP,
        DROPOFF,
        MAINTENANCE_POINT,
        SURVEY_POINT,
        SALVAGE_POINT
    }
}
