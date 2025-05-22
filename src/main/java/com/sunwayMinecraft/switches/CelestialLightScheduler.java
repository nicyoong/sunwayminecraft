package com.sunwayMinecraft.switches;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class CelestialLightScheduler extends BukkitRunnable {
    private final SwitchConfigManager switchConfig;
    private final String worldName;
    private int lastProcessedTime = -1;

    public CelestialLightScheduler(SwitchConfigManager switchConfig, String worldName) {
        this.switchConfig = switchConfig;
        this.worldName = worldName;
    }

    @Override
    public void run() {
        World world = Bukkit.getWorld("world"); // Use your world name
        if (world == null) return;

        long currentTime = world.getTime();

        // Handle midnight (turn off)
        if (isBetween(currentTime, 17980, 18020) && lastProcessedTime != 18000) {
            toggleAllLights(false);
            lastProcessedTime = 18000;
        }
        // Toggle on at 0 ± 1 tick (dawn)
        else if (isBetween(currentTime, 23959, 23999) && lastProcessedTime != 0) {
            toggleAllLights(true);
            lastProcessedTime = 0;
        }
        // Reset tracking if outside both ranges
        else if (!isBetween(currentTime, 17980, 18020) && !isBetween(currentTime, 23959, 23999)) {
            lastProcessedTime = -1;
        }
    }

    private boolean isBetween(long value, long min, long max) {
        return (value >= min && value <= max) ||
                (min > max && (value >= min || value <= max));
    }

    private void toggleAllLights(boolean turnOn) {
        switchConfig.getSwitches().values().forEach(buttonSwitch -> {
            buttonSwitch.lightLocations().forEach(loc -> {
                World locWorld = loc.getWorld();
                if (locWorld == null || !locWorld.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    return;
                }

                Block block = loc.getBlock();
                Material targetMaterial = getTargetMaterial(block.getType(), turnOn);

                if (targetMaterial != null) {
                    block.setType(targetMaterial);
                }
            });
        });
    }

    private Material getTargetMaterial(Material current, boolean turnOn) {
        return turnOn ?
                LightManager.getOriginalMaterial(current) : // Dawn: restore original
                LightManager.getOffMaterial(current);       // Midnight: turn off
    }
}
