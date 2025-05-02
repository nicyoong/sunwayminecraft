package com.sunwayMinecraft.switches;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import java.util.Map;
import org.bukkit.Location;

public class SwitchListener implements Listener {
    private final SwitchManager switchManager;
    private final SwitchConfigManager switchConfig;

    public SwitchListener(SwitchManager switchManager, SwitchConfigManager switchConfig) {
        this.switchManager = switchManager;
        this.switchConfig = switchConfig;
    }

    @EventHandler
    public void onButtonPress(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !block.getType().toString().endsWith("_BUTTON")) return;

        Map<Location, ButtonSwitch> switches = switchConfig.getSwitches();
        ButtonSwitch buttonSwitch = switches.get(block.getLocation());
        if (buttonSwitch != null) {
            switchManager.toggleLights(buttonSwitch, event.getPlayer());
        }
    }
}
