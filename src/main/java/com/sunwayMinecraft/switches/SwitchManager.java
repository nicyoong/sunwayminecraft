package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SwitchManager {
    private final SwitchConfigManager switchConfig;
    private final LightConfigManager lightConfig;

    public SwitchManager(SwitchConfigManager switchConfig, LightConfigManager lightConfig) {
        this.switchConfig = switchConfig;
        this.lightConfig = lightConfig;
    }

    public void toggleLights(ButtonSwitch buttonSwitch, Player player) {
        for (Location loc : buttonSwitch.lightLocations()) {
            Block block = loc.getBlock();
            Material opposite = LightManager.getOppositeMaterial(block.getType());
            if (opposite != null) {
                block.setType(opposite);
            }
        }
    }
}
