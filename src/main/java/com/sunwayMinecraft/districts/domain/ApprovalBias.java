package com.sunwayMinecraft.districts.domain;

public enum ApprovalBias {
    OPEN,
    STANDARD,
    RECOMMENDED_APPROVAL,
    PREMIUM;

    public static ApprovalBias fromString(String value) {
        if (value == null || value.isBlank()) return STANDARD;
        return ApprovalBias.valueOf(value.trim().toUpperCase());
    }
}
