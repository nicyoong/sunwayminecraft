package com.sunwayMinecraft.events.service;

import com.sunwayMinecraft.PluginInitializer;
import com.sunwayMinecraft.SunwayMinecraft;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CityEventsShutdownPersistenceTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        server.addSimpleWorld("world_nether");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void activeEventStateIsPersistedOnDisableAndSurvivesRestart() throws Exception {
        SunwayMinecraft plugin = pluginMock();
        PluginInitializer initializer = new PluginInitializer(plugin);
        CityEventsManager events = initializer.getCityEventsManager();
        assertNotNull(events);

        events.getActiveEvents().add(new ActiveCityEvent("supply_drive",
                Instant.now(), Instant.now().plusSeconds(7200), ActiveCityEvent.TriggerMode.ADMIN));
        File stateFile = new File(dataDirectory.toFile(), "city-event-state.yml");
        assertFalse(stateFile.exists(),
                "precondition: the active event exists only in memory before disable");

        shutdown(plugin, initializer);
        assertTrue(stateFile.exists(),
                "onDisable must persist the current active event state");

        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        server.addSimpleWorld("world_nether");
        PluginInitializer restarted = new PluginInitializer(pluginMock());
        try {
            assertTrue(restarted.getCityEventsManager().isEventActive("supply_drive"),
                    "an event active at shutdown must survive a restart");
        } finally {
            // SQLite connections keep the db files locked on Windows; close so @TempDir can delete them
            if (restarted.getCoinFlipDatabase() != null) {
                restarted.getCoinFlipDatabase().close();
            }
            if (restarted.getAlignmentRepository() != null) {
                restarted.getAlignmentRepository().close();
            }
            if (restarted.getAlignmentSeasonRepository() != null) {
                restarted.getAlignmentSeasonRepository().close();
            }
        }
    }

    // MockBukkit's PluginManagerMock.registerEvents resolves listeners through
    // plugin.getPluginLoader(), so the stub is functionally required. Revisit when
    // Paper completes its plugin-loader rework (both members are deprecated for removal).
    @SuppressWarnings({"removal", "deprecation"})
    private SunwayMinecraft pluginMock() {
        SunwayMinecraft plugin = mock(SunwayMinecraft.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getName()).thenReturn("SunwayMinecraft");
        when(plugin.namespace()).thenReturn("sunwayminecraft");
        when(plugin.getPluginLoader()).thenReturn(new JavaPluginLoader(server));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("CityEventsShutdownPersistenceTest"));
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getConfig()).thenAnswer(invocation -> new YamlConfiguration());
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            File out = new File(dataDirectory.toFile(), name);
            if (!out.exists()) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
                    if (in != null) {
                        Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(anyString(), anyBoolean());
        doCallRealMethod().when(plugin).onDisable();
        return plugin;
    }

    private void shutdown(SunwayMinecraft plugin, PluginInitializer initializer) throws Exception {
        Field field = SunwayMinecraft.class.getDeclaredField("initializer");
        field.setAccessible(true);
        field.set(plugin, initializer);
        plugin.onDisable();
    }
}
