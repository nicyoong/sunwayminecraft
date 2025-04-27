package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager; // Importing the BeaconManager
import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.commands.BeaconCommands; // Importing the BeaconCommands class
import com.sunwayMinecraft.benches.RegionManager;
import com.sunwayMinecraft.commands.BenchesCommands;
import com.sunwayMinecraft.benches.BenchInteractListener;
// Import the other managers and commands classes as needed
import com.sunwayMinecraft.utils.ConfigLoader; // Importing the ConfigLoader
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class SunwayMinecraft extends JavaPlugin {

    private BeaconManager beaconManager; // Instance for managing beacons
    private BenchesConfigManager benchesConfigManager;
    private RegionManager regionManager;
    // Uncomment and initialize these when needed
    // private ChestManager chestManager; // Instance for managing chests
    // private SwitchManager switchManager; // Instance for managing switches
    // private ContainerInspector containerInspector; // Instance for inspecting containers
    // Start instance for the crash game

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().log(Level.INFO, "Enabling SunwayMinecraft plugin...");

        // Load the configuration file
        ConfigLoader.getConfig(this);

        // Initialize the Beacon Manager
        beaconManager = new BeaconManager(this);
        beaconManager.initialize(); // Initializes beacons and starts color changing logic

        // Uncomment and initialize when needed
        // chestManager = new ChestManager(this);
        // chestManager.initialize(); // Initializes chest functionality

        // switchManager = new SwitchManager(this);
        // switchManager.initialize(); // Initializes switches for lights

        // containerInspector = new ContainerInspector(this);
        // containerInspector.initialize(); // Initializes container inspector

        // Initialize bench system
        initializeBenchSystem();

        // Register command handlers
        registerCommands();

        getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
        // Clean up resources if needed
    }

    private void initializeBenchSystem() {
        benchesConfigManager = new BenchesConfigManager(this);
        regionManager = new RegionManager(this, benchesConfigManager);

        BenchInteractListener listener = new BenchInteractListener(this, regionManager);
        listener.register(); // Explicit registration after full initialization
    }

    private void registerCommands() {
        BeaconCommands beaconCommands = new BeaconCommands(beaconManager);

        // Register beacon commands
        registerCommand("pausebeacons", beaconCommands);
        registerCommand("resumebeacons", beaconCommands);
        registerCommand("reloadsunwayconfig", beaconCommands);
        registerCommand("setbeaconticks", beaconCommands);

        BenchesCommands benchesCommands = new BenchesCommands(benchesConfigManager, regionManager);
        registerCommand("listbenches", benchesCommands);
        registerCommand("benchinfo", benchesCommands);
        registerCommand("checkbenchregion", benchesCommands);
        registerCommand("reloadsunwaybenches", benchesCommands);
    }

    // Utility method to register commands with null checks
    private void registerCommand(String commandName, CommandExecutor executor) {
        if (getCommand(commandName) != null) {
            getCommand(commandName).setExecutor(executor);
        } else {
            getLogger().warning("Command '" + commandName + "' not found in plugin.yml!");
        }
    }
}
