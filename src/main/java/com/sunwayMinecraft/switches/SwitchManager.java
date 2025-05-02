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
}
