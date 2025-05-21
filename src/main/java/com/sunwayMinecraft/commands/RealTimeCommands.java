package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.realtime.RealTimeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;

public class RealTimeCommands implements CommandExecutor {
    private final RealTimeManager realTimeManager;

    public RealTimeCommands(RealTimeManager realTimeManager) {
        this.realTimeManager = realTimeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch(cmd.getName().toLowerCase()) {
            case "servertime":
                sendTimeMessage(sender, realTimeManager.getLocalZone(), "Singapore Time (UTC+8)");
                return true;

            case "servertimeutc":
                sendTimeMessage(sender, realTimeManager.getUTCZone(), "UTC Time");
                return true;
        }
        return false;
    }

    private void sendTimeMessage(CommandSender sender, ZoneId zone, String title) {
        String time = realTimeManager.getFormattedTime(zone);
        String date = realTimeManager.getFormattedDate(zone);

        sender.sendMessage("§6§l" + title + " §r§7» §aCurrent time: §e" + time);
        sender.sendMessage("§6§l" + title + " §r§7» §aCurrent date: §e" + date);
    }
}
