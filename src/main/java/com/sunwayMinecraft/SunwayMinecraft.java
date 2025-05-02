package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager;
import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.commands.BeaconCommands;
import com.sunwayMinecraft.benches.RegionManager;
import com.sunwayMinecraft.commands.BenchesCommands;
import com.sunwayMinecraft.benches.BenchInteractListener;
import com.sunwayMinecraft.commands.SwitchesCommands;
import com.sunwayMinecraft.switches.*;
import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public final class SunwayMinecraft extends JavaPlugin {

    // Existing managers
    private BeaconManager beaconManager;
    private BenchesConfigManager benchesConfigManager;
    private RegionManager regionManager;

    // Switch system additions
    private LightConfigManager lightConfigManager;
    private SwitchConfigManager switchConfigManager;
    private SwitchManager switchManager;
    private SwitchListener switchListener;

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "Enabling SunwayMinecraft plugin...");

        // Load main configuration
        ConfigLoader.getConfig(this);

        // Initialize systems
        initializeBeaconSystem();
        initializeBenchSystem();
        initializeSwitchSystem();  // New switch system initialization

        // Register commands
        registerCommands();

        getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
    }

    // Modified command registration
    private void registerCommands() {
        // Beacon commands
        BeaconCommands beaconCommands = new BeaconCommands(beaconManager);
        registerCommand("pausebeacons", beaconCommands);
        registerCommand("resumebeacons", beaconCommands);
        registerCommand("reloadsunwayconfig", beaconCommands);
        registerCommand("setbeaconticks", beaconCommands);

        // Bench commands
        BenchesCommands benchesCommands = new BenchesCommands(benchesConfigManager, regionManager);
        registerCommand("listbenches", benchesCommands);
        registerCommand("benchinfo", benchesCommands);
        registerCommand("checkbenchregion", benchesCommands);
        registerCommand("reloadsunwaybenches", benchesCommands);

        // Switch system commands
        SwitchesCommands switchesCommands = new SwitchesCommands(
                this,
                lightConfigManager,
                switchConfigManager
        );
        registerCommand("scanlights", switchesCommands);
        registerCommand("exportlights", switchesCommands);
        registerCommand("listlightregions", switchesCommands);
        registerCommand("checklightregion", switchesCommands);
        registerCommand("lightinfo", switchesCommands);
        registerCommand("reloadsunwayswitches", switchesCommands);
    }

    // Rest of the existing class remains unchanged
    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
    }

    private void initializeBenchSystem() {
        benchesConfigManager = new BenchesConfigManager(this);
        regionManager = new RegionManager(this, benchesConfigManager);
        BenchInteractListener listener = new BenchInteractListener(this, regionManager);
        listener.register();
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        if (getCommand(commandName) != null) {
            getCommand(commandName).setExecutor(executor);
        } else {
            getLogger().warning("Command '" + commandName + "' not found in plugin.yml!");
        }
    }

    private void initializeBeaconSystem() {
        beaconManager = new BeaconManager(this);
        beaconManager.initialize();
    }

    private void initializeSwitchSystem() {
        // Initialize managers first
        lightConfigManager = new LightConfigManager(this);
        switchConfigManager = new SwitchConfigManager(this);

        // Load initial configurations
        lightConfigManager.reload();
        switchConfigManager.reload();

        // Create switch manager with initialized configs
        switchManager = new SwitchManager(switchConfigManager, lightConfigManager);

        // Register listener with proper dependencies
        SwitchListener listener = new SwitchListener(switchManager, switchConfigManager);
        getServer().getPluginManager().registerEvents(listener, this);
    }
}