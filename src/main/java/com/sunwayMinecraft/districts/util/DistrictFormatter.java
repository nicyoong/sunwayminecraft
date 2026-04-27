package com.sunwayMinecraft.districts.util;

import com.sunwayMinecraft.districts.domain.DistrictType;

public final class DistrictFormatter {

    private DistrictFormatter() {
    }

    public static String formatDistrictType(DistrictType type) {
        if (type == null) {
            return "Unknown";
        }

        return switch (type) {
            case RESIDENTIAL -> "Residential";
            case COMMERCIAL -> "Commercial";
            case OFFICE -> "Office";
            case MARKET -> "Market";
            case ENTERTAINMENT -> "Entertainment";
            case CIVIC -> "Civic";
            case MIXED_USE -> "Mixed-Use";
        };
    }

    public static String formatPrestigeLabel(int prestigeTier) {
        return switch (prestigeTier) {
            case 1 -> "Basic";
            case 2 -> "Standard";
            case 3 -> "Prime";
            case 4 -> "Prestige";
            case 5 -> "Signature";
            default -> prestigeTier <= 0 ? "Unrated" : "Tier " + prestigeTier;
        };
    }
}
