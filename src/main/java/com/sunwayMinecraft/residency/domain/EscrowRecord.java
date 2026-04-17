package com.sunwayMinecraft.residency.domain;

import java.time.Instant;

public class EscrowRecord {
    private final String unitId;
    private final Instant createdAt;
    private final String reason;
    private String status;

    public EscrowRecord(String unitId, Instant createdAt, String reason, String status) {
        this.unitId = unitId;
        this.createdAt = createdAt;
        this.reason = reason;
        this.status = status;
    }

    public String getUnitId() { return unitId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
