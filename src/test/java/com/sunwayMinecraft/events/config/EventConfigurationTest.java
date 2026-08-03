package com.sunwayMinecraft.events.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventConfigurationTest {
    @TempDir
    Path dataDirectory;

    @Test
    void eventConfigurationLoadsKnownBoostsAndIgnoresUnknownCategories() throws Exception {
        Files.writeString(dataDirectory.resolve("city-events.yml"), """
                events:
                  supply:
                    type: SUPPLY_DRIVE
                    name: Supply
                    description: More hauling
                    scope: CITY
                    reward_multiplier: 1.5
                    default_duration_minutes: 20
                    boosted_categories: [HAULING, NOT_A_CATEGORY]
                  invalid:
                    type: NOT_A_TYPE
                """);
        EventConfigManager manager = new EventConfigManager(pluginFor(dataDirectory));

        manager.load();

        CityEventDefinition supply = manager.getEvent("supply");
        assertNotNull(supply);
        assertEquals(1.5, supply.rewardMultiplier());
        assertTrue(supply.boostedCategories().contains(ContractCategory.HAULING));
        assertEquals(1, supply.boostedCategories().size());
        assertNull(manager.getEvent("invalid"));
    }

    @Test
    void eventSettingsUseConfiguredValuesAndSensibleDefaults() throws Exception {
        Files.writeString(dataDirectory.resolve("city-event-settings.yml"), "auto_announcements: false\nmax_active_events: 3\n");
        EventSettingsManager configured = new EventSettingsManager(pluginFor(dataDirectory));
        configured.load();
        assertEquals(3, configured.getMaxActiveEvents());
        assertTrue(!configured.isAutoAnnouncements());

        Path defaults = Files.createDirectory(dataDirectory.resolve("defaults"));
        Files.writeString(defaults.resolve("city-event-settings.yml"), "{}\n");
        EventSettingsManager defaulted = new EventSettingsManager(pluginFor(defaults));
        defaulted.load();
        assertEquals(1, defaulted.getMaxActiveEvents());
        assertTrue(defaulted.isAutoAnnouncements());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("EventConfigurationTest"));
        return plugin;
    }
}
