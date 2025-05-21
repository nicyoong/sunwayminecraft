package com.sunwayMinecraft.realtime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RealTimeManager {
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateFormatter;
    private ZoneId timeZone;

    public RealTimeManager() {
        timeZone = ZoneId.systemDefault();
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    // Separate method to set Singapore time (UTC+8)
    public void setSingaporeTimeZone() {
        timeZone = ZoneId.of("Asia/Singapore");
    }

    public String getFormattedTime() {
        return ZonedDateTime.now(timeZone).format(timeFormatter);
    }

    public String getFormattedDate() {
        return ZonedDateTime.now(timeZone).format(dateFormatter);
    }

//    public String getFormattedTime() {
//        return LocalDateTime.now().format(timeFormatter);
//    }
//
//    public String getFormattedDate() {
//        return LocalDateTime.now().format(dateFormatter);
//    }
}
