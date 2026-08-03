package com.sunwayMinecraft.realtime;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealTimeManagerTest {
    @Test
    void exposesExpectedZonesAndStableDateTimeFormats() {
        RealTimeManager manager = new RealTimeManager();

        assertEquals(ZoneId.of("Asia/Singapore"), manager.getLocalZone());
        assertEquals(ZoneId.of("UTC"), manager.getUTCZone());
        assertTrue(manager.getFormattedTime(manager.getUTCZone()).matches("\\d{2}:\\d{2}:\\d{2}"));
        assertTrue(manager.getFormattedDate(manager.getLocalZone()).matches("\\d{4}-\\d{2}-\\d{2}"));
    }
}
