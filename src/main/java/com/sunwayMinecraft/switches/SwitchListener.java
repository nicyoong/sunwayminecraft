package com.sunwayMinecraft.switches;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import java.util.Map;
import org.bukkit.Location;

public @interface SwitchListener {
    private final SwitchManager switchManager;
    private final SwitchConfigManager switchConfig;

    public SwitchListener(SwitchManager switchManager, SwitchConfigManager switchConfig) {
        this.switchManager = switchManager;
        this.switchConfig = switchConfig;
    }
}
