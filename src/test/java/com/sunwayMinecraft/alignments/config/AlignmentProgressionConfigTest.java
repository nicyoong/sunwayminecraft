package com.sunwayMinecraft.alignments.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentProgressionConfigTest {
    @TempDir
    Path dataDirectory;

    private AlignmentProgressionConfig configFor() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentProgressionConfigTest"));
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
        AlignmentProgressionConfig config = new AlignmentProgressionConfig(plugin);
        config.load();
        return config;
    }

    @Test
    void defaultResourceProvidesFiveRanksWithRisingThresholds() {
        AlignmentProgressionConfig config = configFor();

        assertEquals(5, config.getRanks(null).size());
        assertEquals("initiate", config.getRanks(null).get(0).id());
        assertEquals(0, config.getRanks(null).get(0).minimumReputation());
        assertEquals("archon", config.getRanks(null).get(4).id());
        assertEquals(300, config.getRanks(null).get(4).minimumReputation());
        assertEquals("Archon", config.getRanks(null).get(4).chatSuffix());
    }

    @Test
    void resolveRankPicksTheHighestThresholdTheReputationReaches() {
        AlignmentProgressionConfig config = configFor();

        assertEquals("initiate", config.resolveRank(0, null).get().id());
        assertEquals("initiate", config.resolveRank(24, null).get().id());
        assertEquals("associate", config.resolveRank(25, null).get().id());
        assertEquals("fellow", config.resolveRank(75, null).get().id());
        assertEquals("steward", config.resolveRank(299, null).get().id());
        assertEquals("archon", config.resolveRank(300, null).get().id());
        assertEquals("archon", config.resolveRank(100000, null).get().id());
    }

    @Test
    void allianceRankOverridesReplaceTheWholeSetForThatAlliance() throws Exception {
        Files.writeString(dataDirectory.resolve("alignment-progression.yml"), """
                ranks:
                  initiate:
                    display_name: "Initiate"
                    minimum_reputation: 0
                  archon:
                    display_name: "Archon"
                    minimum_reputation: 300
                alliance_ranks:
                  ironclad_syndicate:
                    overseer:
                      display_name: "Overseer"
                      minimum_reputation: 500
                """);
        AlignmentProgressionConfig config = configFor();

        assertEquals(2, config.getRanks(null).size());
        assertEquals(1, config.getRanks("ironclad_syndicate").size());
        assertEquals("overseer", config.resolveRank(500, "ironclad_syndicate").get().id());
        assertEquals("archon", config.resolveRank(500, "concordat_of_the_dawn").get().id(),
                "alliances without overrides fall back to the global set");
    }

    @Test
    void invalidRanksAreSkippedAndDisabledRanksNeverResolve() throws Exception {
        Files.writeString(dataDirectory.resolve("alignment-progression.yml"), """
                ranks:
                  good:
                    display_name: "Good"
                    minimum_reputation: 10
                  broken:
                    minimum_reputation: 20
                  retired:
                    display_name: "Retired"
                    minimum_reputation: 50
                    enabled: false
                  negative:
                    display_name: "Negative"
                    minimum_reputation: -5
                """);
        AlignmentProgressionConfig config = configFor();

        assertEquals(3, config.getRanks(null).size(), "broken rank must be skipped");
        assertTrue(config.resolveRank(100, null).stream()
                .noneMatch(rank -> rank.id().equals("retired")),
                "disabled ranks must never resolve");
        assertEquals(0, config.getRank(null, "negative").get().minimumReputation(),
                "negative thresholds are clamped to 0");
    }

    @Test
    void seasonAndScoreSettingsAreLoadedWithSafeFallbacks() throws Exception {
        AlignmentProgressionConfig config = configFor();
        assertEquals(30L * 24 * 3_600_000L, config.getSeasonLengthMillis());
        assertEquals(AlignmentProgressionConfig.RestartBehavior.END_PENDING_ON_ENABLE,
                config.getRestartBehavior());
        assertEquals(1, config.getScoreWeights().reputation());
        assertEquals(2, config.getScoreWeights().member());

        Files.writeString(dataDirectory.resolve("alignment-progression.yml"), """
                season:
                  length_days: 7
                  length_hours: 12
                  restart_behavior: continue
                  reset_reputation_on_end: true
                  broadcast_results: false
                score_weights:
                  reputation: 3
                  member: 5
                  district: 10
                  contract: 2
                """);
        AlignmentProgressionConfig custom = configFor();
        assertEquals((7L * 24 + 12) * 3_600_000L, custom.getSeasonLengthMillis());
        assertEquals(AlignmentProgressionConfig.RestartBehavior.CONTINUE, custom.getRestartBehavior());
        assertTrue(custom.isResetReputationOnEnd());
        assertEquals(false, custom.isBroadcastResults());
        assertEquals(3, custom.getScoreWeights().reputation());
        assertEquals(10, custom.getScoreWeights().district());

        Files.writeString(dataDirectory.resolve("alignment-progression.yml"),
                "season:\n  restart_behavior: nonsense\n");
        assertEquals(AlignmentProgressionConfig.RestartBehavior.END_PENDING_ON_ENABLE,
                configFor().getRestartBehavior(), "invalid enum values fall back safely");
    }
}
