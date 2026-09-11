package com.sunwayMinecraft.alignments.config;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentPerksConfigTest {
    @TempDir
    Path dataDirectory;

    private AlignmentPerksConfig configFor() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentPerksConfigTest"));
        doAnswer(invocation -> {
            java.io.File out = new java.io.File(dataDirectory.toFile(),
                invocation.getArgument(0, String.class));
            if (!out.exists()) {
                try (java.io.InputStream in = getClass().getClassLoader()
                        .getResourceAsStream(out.getName())) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean());
        AlignmentPerksConfig config = new AlignmentPerksConfig(plugin);
        config.load();
        return config;
    }

    @Test
    void defaultResourceProvidesThreeSafePerksAndCooldownReduction() {
        AlignmentPerksConfig config = configFor();

        assertTrue(config.isEnabled());
        assertEquals(3, config.getPerks().size());
        assertEquals(PotionEffectType.SPEED, config.getPerks().get("swiftness").effect());
        assertEquals(PotionEffectType.HASTE, config.getPerks().get("diligence").effect());
        assertEquals(PotionEffectType.REGENERATION, config.getPerks().get("vitality").effect());
        assertEquals("fellow", config.getPerks().get("swiftness").minimumRank());
        assertTrue(config.getCooldownReduction().enabled());
        assertEquals(25, config.getCooldownReduction().percent());
    }

    @Test
    void unsupportedEffectTypesAreSkippedWithAWarning() throws Exception {
        Files.writeString(dataDirectory.resolve("alignment-perks.yml"), """
                perks:
                  firebreathing:
                    display_name: "Fire Breathing"
                    effect: fire_resistance
                    minimum_rank: archon
                  speedster:
                    display_name: "Speedster"
                    effect: speed
                    minimum_rank: initiate
                """);
        AlignmentPerksConfig config = configFor();

        assertEquals(1, config.getPerks().size(), "unsupported effects must be skipped");
        assertTrue(config.getPerks().containsKey("speedster"));
    }

    @Test
    void amplifiersAndDurationsAreLoadedAndClamped() throws Exception {
        Files.writeString(dataDirectory.resolve("alignment-perks.yml"), """
                duration_seconds: 30
                max_amplifier: 2
                perks:
                  mega:
                    display_name: "Mega"
                    effect: speed
                    amplifier: 9
                cooldown_reduction:
                  enabled: true
                  minimum_rank: archon
                  percent: 150
                """);
        AlignmentPerksConfig config = configFor();

        assertEquals(30, config.getDurationSeconds());
        assertEquals(2, config.getMaxAmplifier());
        assertEquals(9, config.getPerks().get("mega").amplifier(),
                "raw value is loaded; the perk service caps it at max_amplifier");
        assertEquals(90, config.getCooldownReduction().percent(),
                "reduction is clamped to 90 percent");
    }

    @Test
    void globalDisableIsRespected() throws Exception {
        Files.writeString(dataDirectory.resolve("alignment-perks.yml"), "enabled: false\n");
        assertFalse(configFor().isEnabled());
    }
}
