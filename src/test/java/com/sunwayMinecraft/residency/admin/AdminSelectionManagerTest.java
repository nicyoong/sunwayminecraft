package com.sunwayMinecraft.residency.admin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSelectionManagerTest {
    private ServerMock server;
    private PluginMock plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void selectionSessionTracksBothCornersAndClearStartsAFreshSession() {
        AdminSelectionManager manager = new AdminSelectionManager(plugin);
        UUID player = UUID.randomUUID();
        manager.setPos1(player, new Location(world, 1, 2, 3));
        assertFalse(manager.getSession(player).isComplete());
        manager.setPos2(player, new Location(world, 4, 5, 6));
        assertTrue(manager.getSession(player).isComplete());

        manager.clear(player);
        assertFalse(manager.getSession(player).isComplete());
    }

    @Test
    void onlyTheTaggedWandIsAccepted() {
        AdminSelectionManager manager = new AdminSelectionManager(plugin);
        ItemStack wand = manager.createWandItem();

        assertTrue(manager.isWand(wand));
        assertFalse(manager.isWand(new ItemStack(Material.IRON_SHOVEL)));
        assertFalse(manager.isWand(new ItemStack(Material.STICK)));
        assertFalse(manager.isWand(null));
    }
}
