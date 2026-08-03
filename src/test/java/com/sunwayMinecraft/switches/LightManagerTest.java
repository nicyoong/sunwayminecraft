package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LightManagerTest {
    private ServerMock server;
    private World world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void materialMappingsAreBidirectionalAndRejectUnknownBlocks() {
        assertTrue(LightManager.isLightBlock(Material.SEA_LANTERN));
        assertFalse(LightManager.isLightBlock(Material.DIRT));
        assertEquals(Material.WHITE_CONCRETE, LightManager.getOffMaterial(Material.SEA_LANTERN));
        assertEquals(Material.SEA_LANTERN, LightManager.getOriginalMaterial(Material.WHITE_CONCRETE));
        assertEquals(Material.SEA_LANTERN, LightManager.getOppositeMaterial(Material.WHITE_CONCRETE));
        assertNull(LightManager.getOppositeMaterial(Material.DIRT));
    }

    @Test
    void scanFindsConfiguredLightsAndLeavesNormalBlocksOut() {
        world.getBlockAt(0, 64, 0).setType(Material.SEA_LANTERN);
        world.getBlockAt(1, 64, 0).setType(Material.GLOWSTONE);
        world.getBlockAt(2, 64, 0).setType(Material.DIRT);
        LightRegion region = new LightRegion("test", world, 0, 64, 0, 2, 64, 0);

        List<Block> found = new LightManager(mock(LightConfigManager.class)).scanRegion(region, player);

        assertEquals(2, found.size());
        assertTrue(found.stream().map(Block::getType).allMatch(LightManager::isLightBlock));
    }

    @Test
    void oversizedScanIsRejectedBeforeIteratingTheWorld() {
        LightRegion huge = new LightRegion("huge", world, 0, 0, 0, 200, 383, 200);

        assertThrows(IllegalArgumentException.class,
                () -> new LightManager(mock(LightConfigManager.class)).scanRegion(huge, player));
    }

    @Test
    void lightRegionContainsOnlyItsOwnWorldAndInclusiveBounds() {
        LightRegion region = new LightRegion("bounds", world, 1, 2, 3, 4, 5, 6);
        World other = server.addSimpleWorld("other");

        assertTrue(region.contains(new Location(world, 1, 2, 3)));
        assertTrue(region.contains(new Location(world, 4, 5, 6)));
        assertFalse(region.contains(new Location(world, 5, 5, 6)));
        assertFalse(region.contains(new Location(other, 1, 2, 3)));
    }
}
