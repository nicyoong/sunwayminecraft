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
}
