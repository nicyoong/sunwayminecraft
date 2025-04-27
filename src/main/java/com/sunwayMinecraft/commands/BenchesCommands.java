package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.benches.RegionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BenchesCommands implements CommandExecutor {
    private final BenchesConfigManager configManager;
    private final RegionManager regionManager;

    public BenchesCommands(BenchesConfigManager configManager, RegionManager regionManager) {
        this.configManager = configManager;
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("Usage: /benches reload");
            return false;
        }

        if (!sender.hasPermission("benches.reload")) {
            sender.sendMessage("You don't have permission for this!");
            return true;
        }
}
