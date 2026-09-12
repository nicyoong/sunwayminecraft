package com.sunwayMinecraft.contracts.domain;

import java.time.Instant;
import java.util.UUID;

public class ActiveContract {
    private final UUID playerUuid;
    private final String contractId;
    private final Instant startTime;
    private final Instant expiryTime;
    private double progress; // 0.0 to 1.0
    private String progressState; // pipe-joined stage flags, e.g. "start"
    private int activeId; // SQLite row id; 0 until persisted/reloaded

    public ActiveContract(UUID playerUuid, String contractId, Instant startTime, Instant expiryTime) {
        this.playerUuid = playerUuid;
        this.contractId = contractId;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        this.progress = 0.0;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getContractId() { return contractId; }
    public int getActiveId() { return activeId; }
    public void setActiveId(int activeId) { this.activeId = activeId; }
    public Instant getStartTime() { return startTime; }
    public Instant getExpiryTime() { return expiryTime; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }
    public void completeObjective() { this.progress = 1.0; }
    public boolean isObjectiveComplete() { return progress >= 1.0; }
    public boolean isExpired() { return Instant.now().isAfter(expiryTime); }

    public String getProgressState() { return progressState; }

    public void setProgressState(String progressState) {
        this.progressState = progressState == null || progressState.isBlank() ? null : progressState;
    }

    public boolean hasStage(String stage) {
        if (progressState == null) return false;
        for (String existing : progressState.split("\\|")) {
            if (existing.equals(stage)) return true;
        }
        return false;
    }

    /** Records a completion stage; true only the first time it is seen. */
    public boolean markStage(String stage) {
        if (hasStage(stage)) return false;
        progressState = progressState == null ? stage : progressState + "|" + stage;
        return true;
    }
}
