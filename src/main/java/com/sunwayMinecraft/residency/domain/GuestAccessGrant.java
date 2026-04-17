package com.sunwayMinecraft.residency.domain;

import java.time.Instant;
import java.util.UUID;

public class GuestAccessGrant {
    private final String unitId;
    private final UUID playerId;
    private final GrantType grantType;
    private final Instant startAt;
    private final Instant endAt;
    private final UUID grantedBy;

    public GuestAccessGrant(String unitId, UUID playerId, GrantType grantType, Instant startAt, Instant endAt, UUID grantedBy) {
        this.unitId = unitId;
        this.playerId = playerId;
        this.grantType = grantType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.grantedBy = grantedBy;
    }

    public String getUnitId() { return unitId; }
    public UUID getPlayerId() { return playerId; }
    public GrantType getGrantType() { return grantType; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public UUID getGrantedBy() { return grantedBy; }

    public boolean isActive(Instant now) {
        return (startAt == null || !now.isBefore(startAt))
                && (endAt == null || !now.isAfter(endAt));
    }
}
