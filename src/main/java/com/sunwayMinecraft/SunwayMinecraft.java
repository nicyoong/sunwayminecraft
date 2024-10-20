package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager; // Importing the BeaconManager
// import com.sunwayMinecraft.chest.ChestManager; // Importing the ChestManager
import com.sunwayMinecraft.utils.ConfigLoader; // Importing the ConfigLoader
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public final class SunwayMinecraft extends JavaPlugin {

    private BeaconManager beaconManager; // Instance for managing beacons
    // private ChestManager chestManager; // Instance for managing chests

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().log(Level.INFO, "Enabling SunwayMinecraft plugin...");

        // Load the configuration file
        ConfigLoader.getConfig(this);

        // Initialize the Beacon Manager
        beaconManager = new BeaconManager(this);
        beaconManager.initialize(); // Initializes beacons and starts color changing logic

        // Initialize the Chest Manager (you can implement future functionality)
        // chestManager = new ChestManager(this);
        // chestManager.initialize(); // Initializes chest functionality

        // Register command handler
        CommandHandler commandHandler = new CommandHandler(beaconManager);

        // Register commands with null checks
        if (getCommand("pausebeacons") != null) {
            getCommand("pausebeacons").setExecutor(commandHandler);
        } else {
            getLogger().warning("Command 'pausebeacons' not found in plugin.yml!");
        }

        if (getCommand("resumebeacons") != null) {
            getCommand("resumebeacons").setExecutor(commandHandler);
        } else {
            getLogger().warning("Command 'resumebeacons' not found in plugin.yml!");
        }

        if (getCommand("reloadsunwayconfig") != null) {
            getCommand("reloadsunwayconfig").setExecutor(new CommandHandler(beaconManager));
        } else {
            getLogger().warning("Command 'reloadsunwayconfig' not found in plugin.yml!");
        }

        if (getCommand("setbeaconticks") != null) {
            getCommand("setbeaconticks").setExecutor(new CommandHandler(beaconManager));
        } else {
            getLogger().warning("Command 'setbeaconticks' not found in plugin.yml!");
        }

        getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
        // Clean up resources if needed
    }
}
