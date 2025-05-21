package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.realtime.RealTimeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RealTimeCommands {
    private final RealTimeManager realTimeManager;

    public RealTimeCommands(RealTimeManager realTimeManager) {
        this.realTimeManager = realTimeManager;
    }

}
