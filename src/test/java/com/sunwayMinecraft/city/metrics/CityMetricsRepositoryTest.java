package com.sunwayMinecraft.city.metrics;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CityMetricsRepositoryTest {
    @TempDir
    Path dataDirectory;

    @Test
    void incrementsAndPersistsIndependentMetricKeys() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        CityMetricsRepository writer = new CityMetricsRepository(plugin);
        writer.increment("contracts.completed", 1);
        writer.increment("contracts.completed", 2.5);
        writer.increment("events.started", 1);
        writer.save();

        CityMetricsRepository reader = new CityMetricsRepository(plugin);
        reader.load();

        assertEquals(3.5, reader.get("contracts.completed"));
        assertEquals(1.0, reader.get("events.started"));
        assertEquals(0.0, reader.get("unknown"));
        assertEquals(2, reader.getAll().size());
    }

    @Test
    void missingMetricsFileLoadsAsEmptySnapshot() {
        CityMetricsRepository repository = new CityMetricsRepository(pluginFor(dataDirectory));
        repository.load();

        assertTrue(repository.getAll().isEmpty());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("CityMetricsRepositoryTest"));
        return plugin;
    }
}
