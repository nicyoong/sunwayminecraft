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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("servertime")) {
            sender.sendMessage("§6§lSunway Time §r§7» §aCurrent time: §e" + realTimeManager.getFormattedTime());
            sender.sendMessage("§6§lSunway Time §r§7» §aCurrent date: §e" + realTimeManager.getFormattedDate());
            return true;
        }
        return false;
    }
}
