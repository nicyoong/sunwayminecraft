package com.sunwayMinecraft.residency.access;

import com.sunwayMinecraft.residency.domain.RoleType;
import com.sunwayMinecraft.residency.domain.UnitDefinition;

public class AccessDecision {
    private final boolean allowed;
    private final UnitDefinition unit;
    private final RoleType resolvedRole;
    private final String denialReason;

    private AccessDecision(boolean allowed, UnitDefinition unit, RoleType resolvedRole, String denialReason) {
        this.allowed = allowed;
        this.unit = unit;
        this.resolvedRole = resolvedRole;
        this.denialReason = denialReason;
    }

    public static AccessDecision allow(UnitDefinition unit, RoleType role) { return new AccessDecision(true, unit, role, null); }
    public static AccessDecision deny(UnitDefinition unit, RoleType role, String denialReason) { return new AccessDecision(false, unit, role, denialReason); }
    public boolean isAllowed() { return allowed; }
    public UnitDefinition getUnit() { return unit; }
    public RoleType getResolvedRole() { return resolvedRole; }
    public String getDenialReason() { return denialReason; }
}
