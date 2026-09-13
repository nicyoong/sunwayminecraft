package com.sunwayMinecraft.districts.event;

import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever a district's controlling alignment changes: captures,
 * neutral resets and admin overrides. Other modules can listen without
 * being coupled to the control service.
 */
public class DistrictControlChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public enum ChangeReason { CAPTURE, NEUTRAL_RESET, ADMIN_SET, ADMIN_CLEAR, OVERRIDE_MERGE }

    private final DistrictDefinition district;
    private final String previousAlignmentId;
    private final String newAlignmentId;
    private final String grandAllianceId;
    private final ChangeReason reason;

    public DistrictControlChangeEvent(
            DistrictDefinition district, String previousAlignmentId, String newAlignmentId,
            String grandAllianceId, ChangeReason reason) {
        this.district = district;
        this.previousAlignmentId = previousAlignmentId;
        this.newAlignmentId = newAlignmentId;
        this.grandAllianceId = grandAllianceId;
        this.reason = reason;
    }

    public DistrictDefinition getDistrict() {
        return district;
    }

    public String getDistrictId() {
        return district.getId();
    }

    public String getPreviousAlignmentId() {
        return previousAlignmentId;
    }

    public String getNewAlignmentId() {
        return newAlignmentId;
    }

    public String getGrandAllianceId() {
        return grandAllianceId;
    }

    public ChangeReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
