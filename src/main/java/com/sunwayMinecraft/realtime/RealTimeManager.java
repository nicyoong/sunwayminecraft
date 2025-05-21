package com.sunwayMinecraft.realtime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RealTimeManager {
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateFormatter;

    public TimeManager() {
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    public String getFormattedTime() {
        return LocalDateTime.now().format(timeFormatter);
    }

    public String getFormattedDate() {
        return LocalDateTime.now().format(dateFormatter);
    }
}
