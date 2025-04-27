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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("reloadsunwaybenches")) {
            return false;
        }

        if (!sender.hasPermission("benches.reload")) {
            sender.sendMessage("§cYou don't have permission to reload bench configurations!");
            return true;
        }

        try {
            configManager.reloadConfig();
            regionManager.reloadRegions();
            sender.sendMessage("§aSuccessfully reloaded bench configurations!");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError reloading bench configs: " + e.getMessage());
            return true;
        }
    }
}