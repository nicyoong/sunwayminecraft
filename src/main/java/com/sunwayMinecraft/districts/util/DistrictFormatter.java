package com.sunwayMinecraft.districts.util;

import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;

import java.util.stream.Collectors;

public final class DistrictFormatter {
    private DistrictFormatter() {}

    public static String prestigeLabel(int tier) {
        return switch (tier) {
            case 1 -> "Basic";
            case 2 -> "Standard";
            case 3 -> "Prime";
            case 4 -> "Prestige";
            case 5 -> "Signature";
            default -> tier <= 0 ? "Unrated" : "Tier " + tier;
        };
    }

    public static String typeLabel(DistrictType type) {
        return switch (type) {
            case RESIDENTIAL -> "Residential District";
            case COMMERCIAL -> "Commercial District";
            case OFFICE -> "Office District";
            case MARKET -> "Market District";
            case ENTERTAINMENT -> "Entertainment District";
            case CIVIC -> "Civic District";
            case MIXED_USE -> "Mixed-Use District";
        };
    }

    public static String formatTags(DistrictDefinition district) {
        if (district.getTags().isEmpty()) return "None";
        return district.getTags().stream().collect(Collectors.joining(", "));
    }

    public static String compactLine(DistrictDefinition district) {
        return district.getDisplayName() + " - "
            + prestigeLabel(district.getPrestigeTier()) + " "
            + typeLabel(district.getDistrictType());
    }

    public static String safeSummary(DistrictDefinition district) {
        return district.getPublicSummary().isBlank()
            ? "No public summary configured."
            : district.getPublicSummary();
    }
}
