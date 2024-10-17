package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager; // Importing the BeaconManager
// import com.sunwayMinecraft.chest.ChestManager; // Importing the ChestManager
import com.sunwayMinecraft.utils.ConfigLoader; // Importing the ConfigLoader
import org.bukkit.plugin.java.JavaPlugin;

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

        getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
        // Clean up resources if needed
    }
}
