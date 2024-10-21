package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BeaconManager {
    private final JavaPlugin plugin;
    private final ColorTransition colorTransitionManager;
    private final ConfigurationLoader configLoader;

    public BeaconManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configLoader = new BeaconConfigurationLoader(plugin);
        Map<Location, Integer> beaconColors = configLoader.loadBeacons();
        List<Material> colorCycle = configLoader.getColorCycle();
        this.colorTransitionManager = (ColorTransition) new ColorTransitionManager(plugin, beaconColors, colorCycle);
    }

    public void initialize() {
        int ticksPerTransition = configLoader.getTicksPerTransition();
        colorTransitionManager.startTransition(plugin, ticksPerTransition, configLoader.loadBeacons());
    }

    public void pauseColorTransition() {
        colorTransitionManager.pause();
        plugin.getLogger().log(Level.INFO, "Beacon color transitions paused.");
    }

    public void resumeColorTransition() {
        colorTransitionManager.resume(plugin);
        plugin.getLogger().log(Level.INFO, "Beacon color transitions resumed.");
    }

    public void reloadConfiguration() {
        long ticksPerTransition = configLoader.getTicksPerTransition();
        List<Material> colorCycle = configLoader.getColorCycle();
        // You might want to reset the ColorTransitionManager with new parameters
        plugin.getLogger().log(Level.INFO, "Configuration reloaded.");
    }

    public void setTicksPerTransition(int newTicks) {
        // Update the ticks per transition
        if (newTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, "Invalid ticks per transition value: " + newTicks + ". Must be greater than 0.");
            return; // Prevent invalid tick values
        }

        // Cancel the existing color transition task
        colorTransitionManager.pause(); // Pause the current task to avoid conflicts

        // Update the ticks per transition
        long previousTicks = colorTransitionManager.getTicksPerTransition(); // Retrieve the previous ticks for logging
        plugin.getLogger().log(Level.INFO, "Updating ticks per transition from " + previousTicks + " to " + newTicks);

        // Start the color transition with the new ticks value
        colorTransitionManager.startTransition(plugin, newTicks, configLoader.loadBeacons()); // Restart with new settings

        // Log the update for confirmation
        plugin.getLogger().log(Level.INFO, "Ticks per transition updated to " + newTicks + " ticks.");
    }
}

