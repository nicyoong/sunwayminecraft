package com.sunwayMinecraft.events.domain;

import java.time.Instant;

public class ActiveCityEvent {
    private final String eventId;
    private final Instant startTime;
    private final Instant endTime;
    private final TriggerMode triggerMode;

    public ActiveCityEvent(String eventId, Instant startTime, Instant endTime, TriggerMode triggerMode) {
        this.eventId = eventId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.triggerMode = triggerMode;
    }

    public String getEventId() { return eventId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public TriggerMode getTriggerMode() { return triggerMode; }
    public boolean isExpired() { return Instant.now().isAfter(endTime); }

    public enum TriggerMode {
        SCHEDULED,
        ADMIN
    }
}
