package com.sunwayMinecraft.realtime;

import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RealTimeManager {
    private final JavaPlugin plugin;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateFormatter;

    public TimeManager() {
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    
}
