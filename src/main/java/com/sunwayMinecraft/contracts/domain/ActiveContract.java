package com.sunwayMinecraft.contracts.domain;

import java.time.Instant;
import java.util.UUID;

public class ActiveContract {
    private final UUID playerUuid;
    private final String contractId;
    private final Instant startTime;
    private final Instant expiryTime;
    private double progress; // 0.0 to 1.0

    public ActiveContract(UUID playerUuid, String contractId, Instant startTime, Instant expiryTime) {
        this.playerUuid = playerUuid;
        this.contractId = contractId;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        this.progress = 0.0;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getContractId() { return contractId; }
    public Instant getStartTime() { return startTime; }
    public Instant getExpiryTime() { return expiryTime; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public boolean isExpired() { return Instant.now().isAfter(expiryTime); }
}
