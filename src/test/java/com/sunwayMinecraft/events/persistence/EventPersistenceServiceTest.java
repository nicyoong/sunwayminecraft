package com.sunwayMinecraft.events.persistence;

import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventPersistenceServiceTest {
    @TempDir
    Path dataDirectory;

    @Test
    void savesAndRestoresOnlyUnexpiredEvents() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        EventPersistenceService service = new EventPersistenceService(plugin);
        Instant now = Instant.now();
        ActiveCityEvent active = new ActiveCityEvent("supply", now.minusSeconds(60), now.plusSeconds(3600), ActiveCityEvent.TriggerMode.ADMIN);
        ActiveCityEvent expired = new ActiveCityEvent("old", now.minusSeconds(7200), now.minusSeconds(60), ActiveCityEvent.TriggerMode.SCHEDULED);

        service.save(List.of(active, expired));

        List<ActiveCityEvent> restored = new EventPersistenceService(plugin).load();

        assertEquals(1, restored.size());
        assertEquals("supply", restored.getFirst().getEventId());
        assertEquals(ActiveCityEvent.TriggerMode.ADMIN, restored.getFirst().getTriggerMode());
    }

    @Test
    void malformedPersistedEventIsIgnoredWithoutPreventingOtherEventsFromLoading() throws Exception {
        JavaPlugin plugin = pluginFor(dataDirectory);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("active_events.good.id", "good");
        yaml.set("active_events.good.start", Instant.now().minusSeconds(5).toString());
        yaml.set("active_events.good.end", Instant.now().plusSeconds(60).toString());
        yaml.set("active_events.good.mode", "ADMIN");
        yaml.set("active_events.bad.id", "bad");
        yaml.set("active_events.bad.start", "not-an-instant");
        yaml.set("active_events.bad.end", Instant.now().plusSeconds(60).toString());
        yaml.set("active_events.bad.mode", "ADMIN");
        yaml.save(new File(dataDirectory.toFile(), "city-event-state.yml"));

        List<ActiveCityEvent> restored = new EventPersistenceService(plugin).load();

        assertEquals(1, restored.size());
        assertEquals("good", restored.getFirst().getEventId());
    }

    @Test
    void missingStateFileLoadsAsNoActiveEvents() {
        assertTrue(new EventPersistenceService(pluginFor(dataDirectory)).load().isEmpty());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("EventPersistenceServiceTest"));
        return plugin;
    }
}
