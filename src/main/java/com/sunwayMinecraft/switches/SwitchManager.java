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
        buttonSwitch.lightLocations().forEach(loc -> {
            Block block = loc.getBlock();
            Material current = block.getType();
            Material target = LightManager.isLightBlock(current) ?
                    LightManager.getOffMaterial(current) :
                    LightManager.getOriginalMaterial(current);

            if (target != null) {
                block.setType(target);
            }
        });
    }
}
