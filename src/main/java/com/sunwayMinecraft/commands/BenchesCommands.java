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
        switch (command.getName().toLowerCase()) {
            case "reloadsunwaybenches":
                return handleReload(sender);
            case "listbenches":
                return handleListBenches(sender);
            case "benchinfo":
                return handleBenchInfo(sender, args);
            case "checkbenchregion":
                return handleRegionCheck(sender);
            default:
                return false;
        }
    }
}