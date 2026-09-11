package com.sunwayMinecraft.districts.domain;

/**
 * Per-district contest configuration, resolved from districts.yml overrides
 * with defaults supplied by district-control-settings.yml.
 */
public record DistrictControlProfile(
        boolean contestEnabled,
        int pointsRequiredToCapture,
        double controlPointRadius,
        int contestCooldownSeconds) {
}
