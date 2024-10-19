package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor {

    private final BeaconManager beaconManager;

    // Constructor
    public CommandHandler(BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command is valid
        switch (command.getName().toLowerCase()) {
            case "pausebeacons":
                return handlePauseBeacons(sender);
            case "resumebeacons":
                return handleResumeBeacons(sender);
            case "reloadsunwayconfig":
                return handleReloadConfig(sender);
            default:
                return false; // Invalid command
        }
    }

    private boolean handlePauseBeacons(CommandSender sender) {
        // Check for permission
        if (!sender.hasPermission("beaconmanager.pause")) {
            sender.sendMessage("You do not have permission to pause the beacons.");
            return true;
        }

        // Pause the beacon color transitions
        beaconManager.pauseColorTransition(); // Implement this method in BeaconManager
        sender.sendMessage("Beacon color transitions have been paused.");
        return true;
    }

    private boolean handleResumeBeacons(CommandSender sender) {
        // Check for permission
        if (!sender.hasPermission("beaconmanager.resume")) {
            sender.sendMessage("You do not have permission to resume the beacons.");
            return true;
        }

        // Resume the beacon color transitions
        beaconManager.resumeColorTransition(); // Implement this method in BeaconManager
        sender.sendMessage("Beacon color transitions have been resumed.");
        return true;
    }

    private boolean handleReloadConfig(CommandSender sender) {
        // Check for permission
        if (!sender.hasPermission("beaconmanager.reload")) {
            sender.sendMessage("You do not have permission to reload the configuration.");
            return true;
        }

        // Reload the beacon configuration
        beaconManager.reloadConfiguration(); // Ensure this method exists and is properly implemented
        sender.sendMessage("Configuration reloaded successfully.");
        return true;
    }
}
