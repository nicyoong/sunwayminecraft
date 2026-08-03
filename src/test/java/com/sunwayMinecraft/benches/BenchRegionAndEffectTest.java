package com.sunwayMinecraft.benches;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchRegionAndEffectTest {
    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cuboidRegionNormalizesCornersAndRequiresTheConfiguredWorld() {
        CuboidRegion region = new CuboidRegion("world", new Location(world, 5, 7, 9), new Location(world, 1, 2, 3));
        World other = server.addSimpleWorld("other");

        assertEquals(1, region.getMin().getBlockX());
        assertEquals(9, region.getMax().getBlockZ());
        assertTrue(region.contains(new Location(world, 1, 2, 3)));
        assertTrue(region.contains(new Location(world, 5, 7, 9)));
        assertFalse(region.contains(new Location(other, 1, 2, 3)));
    }

    @Test
    void effectApplierAddsExpectedRegenerationEffect() {
        PlayerMock player = server.addPlayer();
        new EffectApplier().applyRegeneration(player);

        var effect = player.getPotionEffect(PotionEffectType.REGENERATION);
        assertNotNull(effect);
        assertEquals(200, effect.getDuration());
        assertEquals(0, effect.getAmplifier());
    }
}
