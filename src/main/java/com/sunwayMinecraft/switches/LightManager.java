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

    private static final Map<Material, Material> REVERSE_MAPPINGS = createReverseMap();

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

    public static boolean isLightBlock(Material material) {
        return LIGHT_MAPPINGS.containsKey(material);
    }

    public static Material getOppositeMaterial(Material material) {
        return LIGHT_MAPPINGS.getOrDefault(material,
                LIGHT_MAPPINGS.entrySet().stream()
                        .filter(e -> e.getValue() == material)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null)
        );
    }

    private static Map<Material, Material> createReverseMap() {
        Map<Material, Material> reverse = new HashMap<>();
        LIGHT_MAPPINGS.forEach((key, value) -> reverse.put(value, key));
        return Collections.unmodifiableMap(reverse);
    }

    public static Material getOffMaterial(Material light) {
        return LIGHT_MAPPINGS.get(light);
    }

    public static Material getOriginalMaterial(Material off) {
        return REVERSE_MAPPINGS.get(off);
    }
}
