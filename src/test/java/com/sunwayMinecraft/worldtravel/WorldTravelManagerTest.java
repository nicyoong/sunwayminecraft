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

class WorldTravelManagerTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;
    private World lifeWorld;
    private World miningWorld;
    private PlayerMock player;
    private WorldTravelManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        lifeWorld = server.addSimpleWorld("world");
        miningWorld = server.addSimpleWorld("mining");
        player = server.addPlayer();
        player.teleport(new Location(lifeWorld, 5, 70, 5));
        manager = new WorldTravelManager(pluginFor(dataDirectory));
        manager.loadState();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void openAndResetPendingStatesAllowTravelToMiningWorld() {
        assertTrue(manager.teleportToMining(player));
        assertEquals(miningWorld, player.getWorld());

        player.teleport(lifeWorld.getSpawnLocation());
        manager.setMiningWorldState(MiningWorldState.RESET_PENDING);

        assertTrue(manager.teleportToMining(player));
        assertEquals(miningWorld, player.getWorld());
    }

    @Test
    void lockedStateRejectsMiningTravelWithoutMovingThePlayer() {
        manager.setMiningWorldState(MiningWorldState.LOCKED);

        assertFalse(manager.teleportToMining(player));
        assertEquals(lifeWorld, player.getWorld());
    }

    @Test
    void lifeTravelUsesTheConfiguredLifeWorldSpawnWhenNoPersonalRespawnExists() {
        player.teleport(miningWorld.getSpawnLocation());
        player.setRespawnLocation(null);

        assertTrue(manager.teleportToLifeWorld(player));
        assertEquals(lifeWorld, player.getWorld());
        assertEquals(lifeWorld.getSpawnLocation(), player.getLocation());
    }

    @Test
    void stateSurvivesAManagerRestartAndInvalidPersistedStateFallsBackToOpen() throws Exception {
        manager.setMiningWorldState(MiningWorldState.LOCKED);
        WorldTravelManager restarted = new WorldTravelManager(pluginFor(dataDirectory));
        restarted.loadState();
        assertEquals(MiningWorldState.LOCKED, restarted.getMiningWorldState());

        java.nio.file.Files.writeString(dataDirectory.resolve("worldtravel.yml"), "mining-world:\n  state: impossible\n");
        WorldTravelManager invalid = new WorldTravelManager(pluginFor(dataDirectory));
        invalid.loadState();

        assertEquals(MiningWorldState.OPEN, invalid.getMiningWorldState());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("WorldTravelManagerTest"));
        return plugin;
    }
}
