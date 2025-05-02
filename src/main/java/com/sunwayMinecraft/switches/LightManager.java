package com.sunwayMinecraft.switches;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.*;

public class LightManager {
    private static final Map<Material, Material> LIGHT_MAPPINGS = Map.of(
            Material.SEA_LANTERN, Material.WHITE_CONCRETE,
            Material.GLOWSTONE, Material.COBBLESTONE,
            Material.JACK_O_LANTERN, Material.CARVED_PUMPKIN
    );

    private final LightConfigManager configManager;

    public LightManager(LightConfigManager configManager) {
        this.configManager = configManager;
    }
}
