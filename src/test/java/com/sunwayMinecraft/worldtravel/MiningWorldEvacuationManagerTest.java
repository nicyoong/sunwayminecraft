package com.sunwayMinecraft.worldtravel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiningWorldEvacuationManagerTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;
    private World lifeWorld;
    private World miningWorld;
    private PlayerMock player;
    private WorldTravelManager travel;
    private MiningWorldEvacuationManager evacuation;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        lifeWorld = server.addSimpleWorld("world");
        miningWorld = server.addSimpleWorld("mining");
        player = server.addPlayer();
        player.teleport(new Location(miningWorld, 2, 65, 2));
        travel = new WorldTravelManager(pluginFor(dataDirectory));
        evacuation = new MiningWorldEvacuationManager(travel.getPlugin(), travel);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void evacuationTransitionsToResetPendingThenEvacuatesAndLocksTheWorld() {
        assertTrue(evacuation.startEvacuation(5));
        assertTrue(evacuation.isEvacuationRunning());
        assertEquals(MiningWorldState.RESET_PENDING, travel.getMiningWorldState());
        assertFalse(evacuation.startEvacuation(5), "A second evacuation must not replace the first one");

        server.getScheduler().performTicks(121L);

        assertFalse(evacuation.isEvacuationRunning());
        assertEquals(MiningWorldState.LOCKED, travel.getMiningWorldState());
        assertEquals(lifeWorld, player.getWorld());
    }

    @Test
    void cancellationStopsCountdownAndReopensTheWorld() {
        assertFalse(evacuation.startEvacuation(0));
        assertTrue(evacuation.startEvacuation(10));

        assertTrue(evacuation.cancelEvacuation());
        assertFalse(evacuation.isEvacuationRunning());
        assertEquals(0, evacuation.getSecondsRemaining());
        assertEquals(MiningWorldState.OPEN, travel.getMiningWorldState());
        assertFalse(evacuation.cancelEvacuation());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MiningWorldEvacuationManagerTest"));
        return plugin;
    }
}
