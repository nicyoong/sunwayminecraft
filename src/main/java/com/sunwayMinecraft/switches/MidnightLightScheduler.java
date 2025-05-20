package com.sunwayMinecraft.switches;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class MidnightLightScheduler extends BukkitRunnable {
    private final SwitchConfigManager switchConfig;
    private long lastTriggeredTime = -1;

    public MidnightLightScheduler(SwitchConfigManager switchConfig) {
        this.switchConfig = switchConfig;
    }

    @Override
    public void run() {
        World world = Bukkit.getWorld("world"); // Use your world name
        if (world == null) return;

        long time = world.getFullTime() % 24000;

        // Handle midnight (turn off)
        if (time == 18000 && lastTriggeredTime != 18000) {
            toggleAllLights(false); // false = turn off
            lastTriggeredTime = time;
        }
        // Handle dawn (turn on)
        else if (time == 0 && lastTriggeredTime != 0) {
            toggleAllLights(true); // true = turn on
            lastTriggeredTime = time;
        }
        // Reset tracking between cycles
        else if (time != 18000 && time != 0) {
            lastTriggeredTime = -1;
        }
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
