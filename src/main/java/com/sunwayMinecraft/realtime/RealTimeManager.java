package com.sunwayMinecraft.realtime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RealTimeManager {
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateFormatter;
    private ZoneId localZone;

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");

    public RealTimeManager() {
        localZone = SINGAPORE_ZONE;
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    public String getFormattedTime(ZoneId zone) {
        return ZonedDateTime.now(zone).format(timeFormatter);
    }

    public String getFormattedDate(ZoneId zone) {
        return ZonedDateTime.now(zone).format(dateFormatter);
    }

    public ZoneId getLocalZone() {
        return localZone;
    }

    public ZoneId getUTCZone() {
        return UTC_ZONE;
    }
}
