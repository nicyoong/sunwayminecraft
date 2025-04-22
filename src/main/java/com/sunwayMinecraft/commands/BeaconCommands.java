package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.beacon.BeaconManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * The BeaconCommands class implements the CommandExecutor interface to handle
 * different beacon-related commands in the plugin. It interacts with the
 * BeaconManager to control beacon color transitions and configuration settings.
 *
 * The class is responsible for processing commands like pausing, resuming,
 * reloading configurations, and setting the number of ticks per transition.
 * It ensures that users have the necessary permissions to execute each command
 * and provides appropriate feedback.
 *
 * The following commands are handled:
 * - `/pausebeacons`: Pauses the beacon color transition process.
 * - `/resumebeacons`: Resumes the beacon color transition process.
 * - `/reloadsunwayconfig`: Reloads the beacon configuration.
 * - `/setbeaconticks <ticks>`: Sets the number of ticks per transition for
 *   beacon color changes.
 */
public class BeaconCommands implements CommandExecutor {

    private final BeaconManager beaconManager;

    /**
     * Constructor for the BeaconCommands class. Initializes the class with an
     * instance of BeaconManager to manage beacon transitions and configurations.
     * This constructor is used to set up the necessary manager that will be
     * controlled by the commands.
     *
     * @param beaconManager The instance of the BeaconManager that this class
     *                      will interact with.
     */
    public BeaconCommands(BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
    }

    /**
     * Handles the execution of commands related to beacon management. It processes
     * the command name and delegates the execution to the appropriate handler
     * method based on the command.
     *
     * The method checks the command name and calls the corresponding private
     * method to handle the command logic.
     *
     * @param sender The entity issuing the command (e.g., player or console).
     * @param command The command that was executed.
     * @param label The alias used to execute the command.
     * @param args The arguments passed with the command.
     *
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

    /**
     * Handles the `/pausebeacons` command. Pauses the beacon color transition
     * if the sender has the necessary permissions. Provides feedback to the sender
     * based on the success of the action.
     *
     * @param sender The entity issuing the command (e.g., player or console).
     *
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handlePauseBeacons(CommandSender sender) {
        if (!sender.hasPermission("beaconmanager.pause")) {
            sender.sendMessage("You do not have permission to pause the beacons.");
            return true;
        }
        beaconManager.pauseColorTransition();
        sender.sendMessage("Beacon color transitions have been paused.");
        return true;
    }

    /**
     * Handles the `/resumebeacons` command. Resumes the beacon color transition
     * if the sender has the necessary permissions. Provides feedback to the sender
     * based on the success of the action.
     *
     * @param sender The entity issuing the command (e.g., player or console).
     *
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleResumeBeacons(CommandSender sender) {
        if (!sender.hasPermission("beaconmanager.resume")) {
            sender.sendMessage("You do not have permission to resume the beacons.");
            return true;
        }
        beaconManager.resumeColorTransition();
        sender.sendMessage("Beacon color transitions have been resumed.");
        return true;
    }

    /**
     * Handles the `/reloadsunwayconfig` command. Reloads the beacon configuration
     * if the sender has the necessary permissions. Provides feedback to the sender
     * based on the success of the action.
     *
     * @param sender The entity issuing the command (e.g., player or console).
     *
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleReloadConfig(CommandSender sender) {
        if (!sender.hasPermission("beaconmanager.reload")) {
            sender.sendMessage("You do not have permission to reload the configuration.");
            return true;
        }
        beaconManager.reloadConfiguration();
        sender.sendMessage("Configuration reloaded successfully.");
        return true;
    }

    /**
     * Handles the `/setbeaconticks` command. Allows the sender to set the number
     * of ticks per transition for beacon color changes. It validates the argument
     * and ensures that the tick value is within a valid range (1 to 400).
     * Provides feedback based on the outcome of the operation.
     *
     * @param sender The entity issuing the command (e.g., player or console).
     * @param args The arguments passed with the command, including the number of
     *             ticks for the transition.
     *
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleSetBeaconTicks(CommandSender sender, String[] args) {
        if (!sender.hasPermission("beaconmanager.setticks")) {
            sender.sendMessage("You do not have permission to set the beacon ticks.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /setbeaconticks <ticks>");
            return false;
        }

        try {
            int ticks = Integer.parseInt(args[0]);
            if (ticks <= 0 || ticks > 400) {
                sender.sendMessage("Please provide a number of ticks between 1 and 400.");
                return false;
            }
            beaconManager.setTicksPerTransition(ticks);
            sender.sendMessage("Ticks per transition successfully set to " + ticks);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number. Please provide a valid integer.");
        }

        return true;
    }
}
