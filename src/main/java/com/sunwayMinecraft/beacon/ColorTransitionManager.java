package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ColorTransitionManager implements ColorTransition {
    private final JavaPlugin plugin;
    private Map<Location, Integer> beaconColors;
    private List<Material> colorCycle;
    private int currentColorIndex = 0;
    private int currentBinaryCycle = 0;
    private int ticksPerTransition;
    private BukkitTask colorTransitionTask;
    private boolean isRunning = false;

    public ColorTransitionManager(JavaPlugin plugin, Map<Location, Integer> beaconColors, List<Material> colorCycle) {
        this.plugin = plugin;
        this.beaconColors = beaconColors;
        this.colorCycle = colorCycle;
    }

    @Override
    public void startTransition(JavaPlugin plugin, int ticksPerTransition, Map<Location, Integer> beaconColors) {
        this.ticksPerTransition = ticksPerTransition;
        this.beaconColors = beaconColors; // Update beacon colors if needed

        // Ensure any running task is canceled before starting a new one
        if (colorTransitionTask != null && !colorTransitionTask.isCancelled()) {
            colorTransitionTask.cancel();
        }

        // Schedule a new task
        colorTransitionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ColorTransitionManager.this.ticksPerTransition <= 0) return;

                for (Location loc : beaconColors.keySet()) {
                    updateBeaconColor(loc, currentBinaryCycle);
                }

                currentBinaryCycle++;

                if (currentBinaryCycle >= 16) { // 1 << 4 = 16
                    currentBinaryCycle = 0;
                    currentColorIndex = (currentColorIndex + 1) % colorCycle.size();
                }
            }
        }.runTaskTimer(plugin, 0, this.ticksPerTransition);

        isRunning = true;
        plugin.getLogger().log(Level.INFO, "Started beacon color transitions with " + ticksPerTransition + " ticks per transition.");
    }

    @Override
    public void pause() {
        if (colorTransitionTask != null && !colorTransitionTask.isCancelled()) {
            colorTransitionTask.cancel(); // Cancel the scheduled task
            isRunning = false; // Mark as not running
            plugin.getLogger().log(Level.INFO, "Beacon color transitions paused.");
        }
    }

    @Override
    public void resume(JavaPlugin plugin) {
        if (colorTransitionTask == null || colorTransitionTask.isCancelled()) {
            startTransition(plugin, this.ticksPerTransition, beaconColors); // Start the task anew
            plugin.getLogger().log(Level.INFO, "Beacon color transitions resumed.");
        } else {
            plugin.getLogger().warning("Transition is already running. You cannot resume.");
        }
    }

    @Override
    public long getTicksPerTransition() {
        return ticksPerTransition;
    }

    private void updateBeaconColor(Location location, int binaryCycleIndex) {
        int[] binarySequence = {
                0, 16, 24, 20, 28, 18, 26, 22,
                30, 17, 25, 21, 29, 19, 27, 23
        };

        for (int i = 0; i < 5; i++) {
            Location glassBlockLocation = location.clone().add(0, 5 - i, 0);
            Block glassBlock = glassBlockLocation.getBlock();

            Material newColor = colorCycle.get(currentColorIndex);
            Material oldColor = colorCycle.get((currentColorIndex + colorCycle.size() - 1) % colorCycle.size());

            int colorPosition = binarySequence[binaryCycleIndex % binarySequence.length];
            int bitState = (colorPosition & (1 << i)) != 0 ? 1 : 0;

            glassBlock.setType(bitState == 1 ? newColor : oldColor);
        }

        Block block = location.getBlock();
        if (block.getType() == Material.BEACON) {
            Beacon beacon = (Beacon) block.getState();
            beacon.update();
        }
    }
}
