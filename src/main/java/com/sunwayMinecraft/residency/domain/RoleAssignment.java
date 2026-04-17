package com.sunwayMinecraft.residency.domain;

import java.time.Instant;
import java.util.UUID;

public class RoleAssignment {
    private final String unitId;
    private final RoleType roleType;
    private final UUID playerId;
    private final UUID grantedBy;
    private final Instant grantedAt;
    private final Instant expiresAt;
    private final String reason;

    public RoleAssignment(String unitId, RoleType roleType, UUID playerId, UUID grantedBy, Instant grantedAt, Instant expiresAt, String reason) {
        this.unitId = unitId;
        this.roleType = roleType;
        this.playerId = playerId;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    public String getUnitId() { return unitId; }
    public RoleType getRoleType() { return roleType; }
    public UUID getPlayerId() { return playerId; }
    public UUID getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getReason() { return reason; }
    public boolean isExpired(Instant now) { return expiresAt != null && expiresAt.isBefore(now); }
}
