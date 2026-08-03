package com.sunwayMinecraft.petfinder;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PetFinderManagerTest {
    private ServerMock server;
    private PluginMock plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void preventsOverlappingSearchesAndReleasesTheLockWhenAnEmptySearchCompletes() {
        PetFinderManager manager = new PetFinderManager(plugin);
        CommandSender sender = mock(CommandSender.class);

        manager.startSearch(sender, null, null);
        manager.startSearch(sender, null, null);
        verify(sender).sendMessage("§cA search is already in progress.");

        server.getScheduler().performOneTick();
        manager.startSearch(sender, null, null);

        verify(sender, times(2)).sendMessage("§aStarting pet search...");
    }
}
