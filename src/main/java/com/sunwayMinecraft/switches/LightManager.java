package com.sunwayMinecraft.switches;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * The LightManager class provides utilities for managing “light” blocks within
 * configured regions. It uses mappings between light-emitting materials and their
 * non-light counterparts to identify, scan, and convert blocks in a region.
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>{@link #scanRegion(LightRegion, Player)}: Scans a region for blocks
 *       whose material is defined in the light mappings.</li>
 *   <li>{@link #isLightBlock(Material)}: Checks if a given material is a light block.</li>
 *   <li>{@link #getOppositeMaterial(Material)}: Retrieves the paired material
 *       (light ↔ non-light) for a given material.</li>
 *   <li>{@link #getOffMaterial(Material)}: Gets the non-light version of a light block.</li>
 *   <li>{@link #getOriginalMaterial(Material)}: Gets the light-emitting version
 *       for a non-light block.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * LightConfigManager configMgr = new LightConfigManager(plugin);
 * configMgr.reload();
 * LightManager lightMgr = new LightManager(configMgr);
 * LightRegion region = configMgr.getRegions().get("spawnArea");
 * List<Block> lights = lightMgr.scanRegion(region, player);
 * }</pre>
 */
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

    /**
     * Scans the specified {@link LightRegion} for all blocks whose material matches
     * one of the configured light mappings.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Calculates the total volume of the region as
     *       (maxX - minX + 1) × (maxY - minY + 1) × (maxZ - minZ + 1).</li>
     *   <li>If the volume exceeds 1,000,000 blocks, throws
     *       {@link IllegalArgumentException} to prevent excessively large scans.</li>
     *   <li>Iterates over each block coordinate within the region bounds.</li>
     *   <li>For each block, checks if its type is present in the {@code LIGHT_MAPPINGS} key set.</li>
     *   <li>Adds matching blocks to the result list.</li>
     * </ol>
     *
     * <p>This method is typically used to locate all light-emitting blocks within a region
     * so that they can be toggled or processed further.
     *
     * @param region the {@link LightRegion} defining the world and bounding coordinates to scan
     * @param player the {@link Player} requesting the scan (can be used for context or permissions)
     * @return a {@link List} of {@link Block} instances whose material is a configured light block
     * @throws IllegalArgumentException if the region contains more than 1,000,000 blocks
     */
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
