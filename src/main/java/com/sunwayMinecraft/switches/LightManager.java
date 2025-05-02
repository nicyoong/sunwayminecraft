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

    public List<Block> scanRegion(LightRegion region, Player player) throws IllegalArgumentException {
        World world = region.world();
        int volume = (region.maxX() - region.minX() + 1) *
                (region.maxY() - region.minY() + 1) *
                (region.maxZ() - region.minZ() + 1);

        if (volume > 1_000_000) {
            throw new IllegalArgumentException("Region is too large! Max 1M blocks");
        }

        List<Block> lightBlocks = new ArrayList<>();
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (LIGHT_MAPPINGS.containsKey(block.getType())) {
                        lightBlocks.add(block);
                    }
                }
            }
        }
        return lightBlocks;
    }
}
