package com.sunwayMinecraft.switches;

import org.bukkit.Bukkit;
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
        World world = Bukkit.getWorld("world"); // Change if using different world
        if (world == null) return;

        long time = world.getFullTime() % 24000;

        // Check if it's midnight (18000 ticks) and we haven't triggered yet
        if (time == 18000 && lastTriggeredTime != 18000) {
            toggleAllLights();
            lastTriggeredTime = time;
        } else if (time != 18000) {
            lastTriggeredTime = -1; // Reset tracking
        }
    }
}
