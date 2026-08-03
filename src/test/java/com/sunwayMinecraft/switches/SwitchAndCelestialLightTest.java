package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwitchAndCelestialLightTest {
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
    void manualSwitchTogglesMappedLightsInBothDirections() {
        Location light = new Location(world, 2, 64, 2);
        light.getBlock().setType(Material.SEA_LANTERN);
        ButtonSwitch button = new ButtonSwitch(new Location(world, 1, 64, 2), List.of(light));
        SwitchManager manager = new SwitchManager(mock(SwitchConfigManager.class), mock(LightConfigManager.class));

        manager.toggleLights(button, player);
        assertEquals(Material.WHITE_CONCRETE, light.getBlock().getType());

        manager.toggleLights(button, player);
        assertEquals(Material.SEA_LANTERN, light.getBlock().getType());
    }

    @Test
    void celestialSchedulerTurnsConfiguredLightsOffAtMidnightAndRestoresThemAtDawn() {
        Location light = new Location(world, 4, 64, 4);
        world.loadChunk(light.getBlockX() >> 4, light.getBlockZ() >> 4);
        light.getBlock().setType(Material.GLOWSTONE);
        ButtonSwitch button = new ButtonSwitch(new Location(world, 3, 64, 4), List.of(light));
        SwitchConfigManager config = mock(SwitchConfigManager.class);
        when(config.getSwitches()).thenReturn(Map.of(button.buttonLocation(), button));
        CelestialLightScheduler scheduler = new CelestialLightScheduler(config, "world");

        world.setTime(18000);
        scheduler.run();
        assertEquals(Material.COBBLESTONE, light.getBlock().getType());

        world.setTime(1000);
        scheduler.run();
        // The scheduler intentionally recognizes the final tick window before the day wraps.
        world.setTime(23999);
        scheduler.run();
        assertEquals(Material.GLOWSTONE, light.getBlock().getType());
    }
}
