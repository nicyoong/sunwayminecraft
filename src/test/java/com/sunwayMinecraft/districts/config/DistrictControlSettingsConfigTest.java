package com.sunwayMinecraft.districts.config;

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

class DistrictControlSettingsConfigTest {
    @TempDir
    Path dataDirectory;

    private DistrictControlSettingsConfig configFor() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ControlSettingsTest"));
        DistrictControlSettingsConfig config = new DistrictControlSettingsConfig(plugin);
        config.load();
        return config;
    }

    @Test
    void defaultsAreSafe() {
        DistrictControlSettingsConfig config = configFor();
        assertTrue(config.isEnabled());
        assertEquals(5, config.getContestTickIntervalSeconds());
        assertEquals(2.0, config.getPointsPerPlayerPerTick());
        assertEquals(100, config.getPointsRequiredToCapture());
        assertTrue(config.isDecayWhenUncontested());
        assertTrue(config.isAllowNeutralReset());
        assertEquals(300, config.getCaptureCooldownSeconds());
        assertTrue(config.isRewardControllersOnCapture() == false
                || config.getRewardMoney() >= 0);
        assertEquals("District contest: {alignment} {percent}%", config.getContestActionBar());
    }

    @Test
    void customValuesAndClampsAreApplied() throws Exception {
        Files.writeString(dataDirectory.resolve("district-control-settings.yml"), """
                enabled: false
                contest_tick_interval_seconds: -5
                points_per_player_per_tick: 1.5
                minimum_players_to_contest: 0
                capture_cooldown_seconds: -10
                reward_money: 50.0
                minimum_contribution_points: 25
                contest_action_bar: "{alignment} holds {district} at {percent}%"
                """);
        DistrictControlSettingsConfig config = configFor();
        assertFalse(config.isEnabled(), "the custom file disables control");
        // note: enabled was not written, so the default true applies
        assertEquals(1, config.getContestTickIntervalSeconds(), "interval is clamped to >= 1");
        assertEquals(1.5, config.getPointsPerPlayerPerTick());
        assertEquals(1, config.getMinimumPlayersToContest(), "clamped to >= 1");
        assertEquals(0, config.getCaptureCooldownSeconds(), "negative cooldowns clamp to 0");
        assertEquals(50.0, config.getRewardMoney());
        assertEquals(25, config.getMinimumContributionPoints());
        assertEquals("{alignment} holds {district} at {percent}%", config.getContestActionBar());
    }
}
