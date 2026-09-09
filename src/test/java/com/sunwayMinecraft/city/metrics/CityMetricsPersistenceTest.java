package com.sunwayMinecraft.city.metrics;

import com.sunwayMinecraft.PluginInitializer;
import com.sunwayMinecraft.SunwayMinecraft;
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
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CityMetricsPersistenceTest {
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
    void periodicTaskAndShutdownPersistCountersAcrossServerRestart() throws Exception {
        SunwayMinecraft plugin = pluginMock();
        PluginInitializer initializer = new PluginInitializer(plugin);
        CityMetricsManager metrics = initializer.getCityMetricsManager();
        assertNotNull(metrics);

        metrics.increment("contracts.completed", 3);

        server.getScheduler().performTicks(6000);
        File metricsFile = new File(dataDirectory.toFile(), "city-metrics.yml");
        assertTrue(metricsFile.exists(),
                "city-metrics.yml should be written by the periodic save task");

        metrics.increment("events.started", 2);
        shutdown(plugin, initializer);
        MockBukkit.unmock();

        server = MockBukkit.mock();
        PluginInitializer restarted = new PluginInitializer(pluginMock());
        assertEquals(3.0, restarted.getCityMetricsManager().getSnapshot().getMetric("contracts.completed"),
                "counter saved by the periodic task must survive a restart");
        assertEquals(2.0, restarted.getCityMetricsManager().getSnapshot().getMetric("events.started"),
                "counter saved on shutdown must survive a restart");
    }

    private SunwayMinecraft pluginMock() throws Exception {
        SunwayMinecraft plugin = mock(SunwayMinecraft.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getName()).thenReturn("SunwayMinecraft");
        when(plugin.namespace()).thenReturn("sunwayminecraft");
        when(plugin.getPluginLoader()).thenReturn(new JavaPluginLoader(server));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("CityMetricsPersistenceTest"));
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
