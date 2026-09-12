package com.sunwayMinecraft.contracts.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractDiplomacySettingsTest {
    @TempDir
    Path dataDirectory;

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractDiplomacySettingsTest"));
        return plugin;
    }

    private ContractDiplomacySettings load() throws Exception {
        ContractDiplomacySettings settings = new ContractDiplomacySettings(plugin());
        settings.load();
        return settings;
    }

    @Test
    void absentFileYieldsSafeDefaultsAndSabotageDisabled() throws Exception {
        // contract-diplomacy.yml does not exist in the temp dir; saveResource is a
        // mock no-op, so load() must fall back to safe defaults
        ContractDiplomacySettings settings = load();
        assertTrue(settings.isEnabled());
        assertEquals(5, settings.getInfluencePerCompletion());
        assertEquals(1.0, settings.getDiplomaticContractMultiplier());
        assertFalse(settings.isSabotageEnabled(), "sabotage must default off");
        assertEquals(600, settings.getSabotageCooldownSeconds());
        assertEquals(0.5, settings.getSabotageSuccessChance());
        assertEquals(3, settings.getMaxActiveDynamicContracts());
        assertEquals(0, settings.getEmergencyIntervalMinutes());
    }

    @Test
    void successChanceIsClampedIntoUnitRange() throws Exception {
        Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"),
                "sabotage_success_chance: 2.5\n");
        assertEquals(1.0, load().getSabotageSuccessChance(), "clamped to 1.0 upper bound");

        Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"),
                "sabotage_success_chance: -3\n");
        assertEquals(0.0, load().getSabotageSuccessChance(), "clamped to 0.0 lower bound");
    }

    @Test
    void emergencyGenerationSectionIsRead() throws Exception {
        Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"),
                "emergency_generation:\n  interval_minutes: 15\n  template: supply_drop\n");
        ContractDiplomacySettings settings = load();
        assertEquals(15, settings.getEmergencyIntervalMinutes());
        assertEquals("supply_drop", settings.getEmergencyTemplate());
    }
}
