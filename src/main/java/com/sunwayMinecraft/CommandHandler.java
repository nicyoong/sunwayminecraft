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
            case "setbeaconticks":
                return handleSetBeaconTicks(sender, args);
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

    private boolean handleSetBeaconTicks(CommandSender sender, String[] args) {
        // Check for permission
        if (!sender.hasPermission("beaconmanager.setticks")) {
            sender.sendMessage("You do not have permission to set the beacon ticks.");
            return true;
        }

        // Check if the correct number of arguments is provided
        if (args.length != 1) {
            sender.sendMessage("Usage: /setbeaconticks <ticks>");
            return false;
        }

        try {
            // Parse the ticks value from the command argument
            int ticks = Integer.parseInt(args[0]);

            // Ensure ticks is a positive number and does not exceed 400
            if (ticks <= 0 || ticks > 400) {
                sender.sendMessage("Please provide a number of ticks between 1 and 400.");
                return false;
            }

            // Update the ticks per transition in BeaconManager
            beaconManager.setTicksPerTransition(ticks);
            sender.sendMessage("Ticks per transition successfully set to " + ticks);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number. Please provide a valid integer.");
        }

        return true;
    }

}
