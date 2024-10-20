package com.sunwayMinecraft.beacon;

import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BeaconManager {
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private BukkitRunnable colorTransitionTask; // Store the scheduled task

    // Stores beacon locations and their current color state
    private final Map<Location, Integer> beaconColors;

    // Ticks per color transition
    private long ticksPerTransition;

    private int currentColorIndex = 0; // Keep track of the current color index
    private Material currentOldColor; // To store the old color

    // Dynamic color cycle list loaded from config
    private List<Material> colorCycle;

    // Track the current step in the binary cycle
    private int currentBinaryCycle = 0;

    // Constructor to initialize the BeaconManager
    public BeaconManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler(); // Get the scheduler from the server

        this.beaconColors = new HashMap<>();

        // Load ticks per transition from config
        this.ticksPerTransition = getTicksPerTransitionFromConfig();

        // Load colors from config
        this.colorCycle = getColorCycleFromConfig();
    }

    private void loadBeaconsFromFile() {
        // Load the beacon locations configuration
        FileConfiguration beaconConfig = ConfigLoader.loadBeaconLocations(plugin);
        List<Map<?, ?>> beacons = beaconConfig.getMapList("beaconLocations");

        for (Map<?, ?> beaconData : beacons) {
            try {
                String worldName = (String) beaconData.get("world");
                double x = ((Number) beaconData.get("x")).doubleValue();
                double y = ((Number) beaconData.get("y")).doubleValue();
                double z = ((Number) beaconData.get("z")).doubleValue();

                // Convert to a Location object
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);

                // Add the beacon location
                addBeacon(location);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Invalid beacon location in beaconlocations.yml: " + beaconData);
            }
        }
    }

    public void addBeacon(Location location) {
        beaconColors.put(location, 0); // Start with the default color index
    }

    // Initialize the manager and start color transitions
    public void initialize() {
        loadBeaconsFromFile(); // Load beacons from config file
        colorCycle = getColorCycleFromConfig(); // Ensure this is correctly called to populate colors
        currentOldColor = colorCycle.get(0); // Set the initial old color to the first color
        startColorTransitionTask();
    }

    // Start a task to handle color transitions for beacons
    private void startColorTransitionTask() {

        // Cancel any existing task to prevent multiple tasks from running
        if (colorTransitionTask != null) {
            colorTransitionTask.cancel();
        }

        colorTransitionTask = new BukkitRunnable() {

            @Override
            public void run() {
                // Skip if transitions are disabled
                if (ticksPerTransition <= 0) return;

                // Update the color for each beacon
                for (Location loc : beaconColors.keySet()) {
                    updateBeaconColor(loc, currentBinaryCycle);
                }

                // Increment the binary cycle
                currentBinaryCycle++;

                // If we've completed a full cycle through all binary combinations ((32-1) for 5 blocks)
                if (currentBinaryCycle >= 16) { // 1 << 4 = 16
                    currentBinaryCycle = 0; // Reset the binary cycle
                    currentColorIndex = (currentColorIndex + 1) % colorCycle.size(); // Move to the next color
                }
            }
        };

        colorTransitionTask.runTaskTimer(plugin, 0, ticksPerTransition); // Schedule the task
    }

    // New method to retrieve the color transition task for testing
    public BukkitRunnable getColorTransitionTask() {
        return colorTransitionTask;
    }

    public void testUpdateBeaconColor(Location location, int cycleStep) {
        updateBeaconColor(location, cycleStep); // Calls the private method
    }

    // Update the color of a specific beacon based on the binary cycle index
    private void updateBeaconColor(Location location, int binaryCycleIndex) {
        // Define the binary sequence mapping
        // Easter egg.
        // If we write the numbers out in 5 bit binary
        // it produces a beautiful pattern. Try it!
        int[] binarySequence = {
                0, 16, 24, 20, 28, 18, 26, 22,
                30, 17, 25, 21, 29, 19, 27, 23
        };

        // Loop through the 5 bits (for 5 glass blocks)
        for (int i = 0; i < 5; i++) {  // Start from the bottom block (0, 1, 0) to (0, 5, 0)
            // Determine the glass block location
            Location glassBlockLocation = location.clone().add(0, 5 - i, 0); // (0, 5 - i, 0)
            Block glassBlock = glassBlockLocation.getBlock();

            // Get the current color for the block based on the binary cycle index
            Material newColor = colorCycle.get(currentColorIndex);
            Material oldColor = colorCycle.get((currentColorIndex + colorCycle.size() - 1) % colorCycle.size()); // Previous color

            // Get the index from the binary sequence based on the current binaryCycleIndex
            int colorPosition = binarySequence[binaryCycleIndex % binarySequence.length];

            // Determine the bit state based on the binarySequence mapping
            int bitState = (colorPosition & (1 << i)) != 0 ? 1 : 0; // Use colorPosition instead of binaryCycleIndex

            // Set the block color based on the bit state
            if (bitState == 1) {
                // If the bit is set, use the new color
                glassBlock.setType(newColor);
            } else {
                // If the bit is not set, use the old color
                glassBlock.setType(oldColor);
            }
        }

        // Update the beacon block to apply changes
        Block block = location.getBlock();
        if (block.getType() == Material.BEACON) {
            Beacon beacon = (Beacon) block.getState();
            beacon.update();
        }
    }




    // Get the color index based on the cycle step
    private int getColorIndex(int cycleStep) {
        // Cycle through the colors based on the config list
        return cycleStep % colorCycle.size();
    }

    // Retrieve ticks per transition from the config (default to 20 if not set)
    private int getTicksPerTransitionFromConfig() {
        return plugin.getConfig().getInt("ticksPerTransition", 20);
    }

    // Retrieve the list of colors from the config file
    private List<Material> getColorCycleFromConfig() {
        List<String> colorNames = plugin.getConfig().getStringList("beaconColors");
        List<Material> materials = new ArrayList<>();

        // Convert string names to Material enum and validate
        for (String colorName : colorNames) {
            try {
                // Convert string to Material enum
                Material material = Material.valueOf(colorName.toUpperCase());

                // Validate the material type
                if (material.toString().contains("STAINED_GLASS")) {
                    materials.add(material);
                } else {
                    plugin.getLogger().log(Level.WARNING,
                            "Invalid glass color in config: " + colorName);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid material in config: " + colorName);
            }
        }

        // Fallback to a default color if none are valid
        if (materials.isEmpty()) {
            plugin.getLogger().log(Level.SEVERE,
                    "No valid beacon colors found in config, using default MAGENTA_STAINED_GLASS.");
            materials.add(Material.MAGENTA_STAINED_GLASS); // Default color
        }

        return materials;
    }

    // Public method to dynamically update ticks per transition and restart the task
    public void setTicksPerTransition(long newTicks) {
        // Update the ticks per transition
        this.ticksPerTransition = newTicks;

        // Restart the color transition task to apply the new timing
        startColorTransitionTask();

        // Log the update for confirmation
        plugin.getLogger().log(Level.INFO, "Ticks per transition updated to " + newTicks + " ticks.");
    }

    public void pauseColorTransition() {
        if (colorTransitionTask != null) {
            colorTransitionTask.cancel(); // Cancel the current task, pausing transitions
            plugin.getLogger().log(Level.INFO, "Beacon color transitions paused.");
        }
    }

    public void resumeColorTransition() {
        if (colorTransitionTask == null || colorTransitionTask.isCancelled()) {
            startColorTransitionTask(); // Restart the task from where it left off
            plugin.getLogger().log(Level.INFO, "Beacon color transitions resumed.");
        }
    }

    // Optionally, add a method to reload configuration dynamically
    public void reloadConfiguration() {
        this.ticksPerTransition = getTicksPerTransitionFromConfig();
        this.colorCycle = getColorCycleFromConfig();
        loadBeaconsFromFile(); // Reload the beacons from the file
        plugin.getLogger().log(Level.INFO, "Configuration reloaded.");
    }
}
