package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.SeasonState;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignmentSeasonServiceTest {

    @org.junit.jupiter.api.AfterEach
    void closeRepository() {
        if (seasonRepository != null) {
            seasonRepository.close();
        }
    }
    @TempDir
    Path dataDirectory;

    private AlignmentProgressionConfig progressionConfig;
    private AlignmentSeasonRepository seasonRepository;
    private AlignmentRepository membershipRepository;
    private AlignmentConfigManager configManager;
    private AlignmentMembershipCache cache;
    private AlignmentSeasonService service;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentSeasonServiceTest"));
        progressionConfig = mock(AlignmentProgressionConfig.class);
        when(progressionConfig.getSeasonLengthMillis()).thenReturn(30L * 24 * 3_600_000L);
        when(progressionConfig.getRestartBehavior())
                .thenReturn(AlignmentProgressionConfig.RestartBehavior.END_PENDING_ON_ENABLE);
        when(progressionConfig.isResetReputationOnEnd()).thenReturn(false);
        when(progressionConfig.isBroadcastResults()).thenReturn(false);
        seasonRepository = new AlignmentSeasonRepository(plugin);
        membershipRepository = mock(AlignmentRepository.class);
        configManager = mock(AlignmentConfigManager.class);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        cache = mock(AlignmentMembershipCache.class);
        service = new AlignmentSeasonService(
                plugin, progressionConfig, seasonRepository, membershipRepository,
                configManager, cache);
    }

    @Test
    void snapshotCreationRecordsAlignmentAndPlayerDataForTheCurrentSeason() {
        UUID playerUuid = UUID.randomUUID();
        seasonRepository.saveSeasonState(new SeasonState("season-1", System.currentTimeMillis()));
        when(membershipRepository.getAlignmentTotals())
                .thenReturn(Map.of("azure_hearth", new long[] {420, 7}));
        when(membershipRepository.getAllMemberships()).thenReturn(List.of(
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L)));

        assertTrue(service.forceSnapshot());

        List<AlignmentSeasonRepository.AlignmentSnapshot> latest = seasonRepository.getLatestSnapshots();
        assertEquals(1, latest.size());
        assertEquals("season-1", latest.get(0).seasonId());
        assertEquals("azure_hearth", latest.get(0).alignmentId());
        assertEquals("concordat_of_the_dawn", latest.get(0).grandAllianceId(),
                "snapshots record the grand alliance from the config");
        assertEquals(420, latest.get(0).totalReputation());
        assertEquals(7, latest.get(0).memberCount());

        List<AlignmentSeasonRepository.PlayerSeasonRecord> history =
                seasonRepository.getPlayerSeasonHistory(playerUuid);
        assertEquals(1, history.size());
        assertEquals("azure_hearth", history.get(0).alignmentId());
    }

    @Test
    void endingASeasonSnapshotsThenOpensTheNextSeason() {
        seasonRepository.saveSeasonState(new SeasonState("season-1", System.currentTimeMillis()));
        when(membershipRepository.getAlignmentTotals())
                .thenReturn(Map.of("azure_hearth", new long[] {100, 3}));
        when(membershipRepository.getAllMemberships()).thenReturn(List.of());

        assertTrue(service.endSeason("test"));

        Optional<SeasonState> current = service.getCurrentSeason();
        assertTrue(current.isPresent());
        assertEquals("season-2", current.get().seasonId(),
                "ending a season must open the next numbered season");
        verify(cache, never()).clearAll(); // reset_reputation_on_end is off
    }

    @Test
    void seasonEndHonoursTheResetReputationSetting() {
        seasonRepository.saveSeasonState(new SeasonState("season-3", System.currentTimeMillis()));
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of());
        when(membershipRepository.getAllMemberships()).thenReturn(List.of());
        when(progressionConfig.isResetReputationOnEnd()).thenReturn(true);

        assertTrue(service.endSeason("reset test"));

        verify(cache).clearAll();
        verify(cache).loadOnlinePlayers();
        assertEquals("season-4", service.getCurrentSeason().get().seasonId());
    }

    @Test
    void periodicCheckEndsTheSeasonOnlyAfterTheConfiguredLength() {
        long length = 30L * 24 * 3_600_000L;
        when(progressionConfig.getSeasonLengthMillis()).thenReturn(length);

        seasonRepository.saveSeasonState(new SeasonState("season-1", System.currentTimeMillis() - 1000));
        service.checkSeason();
        assertEquals("season-1", service.getCurrentSeason().get().seasonId(),
                "a young season must not be ended by the periodic check");

        seasonRepository.saveSeasonState(
                new SeasonState("season-1", System.currentTimeMillis() - length - 1000));
        service.checkSeason();
        assertEquals("season-2", service.getCurrentSeason().get().seasonId(),
                "an ended season must be closed by the periodic check");
    }

    @Test
    void resetSeasonStartsANewSeasonWithoutSnapshotsOrResets() {
        seasonRepository.saveSeasonState(new SeasonState("season-1", System.currentTimeMillis()));

        assertTrue(service.resetSeason());

        assertEquals("season-2", service.getCurrentSeason().get().seasonId());
        assertTrue(seasonRepository.getLatestSnapshots().isEmpty(),
                "reset must not snapshot");
        verify(cache, never()).clearAll();
    }

    @Test
    void startCreatesTheFirstSeasonWhenNoStateExists() {
        service.start();
        assertTrue(service.getCurrentSeason().isPresent());
        assertEquals("season-1", service.getCurrentSeason().get().seasonId());
    }

    @Test
    void startEndsAPendingSeasonWhenConfiguredToDoSo() {
        long length = 30L * 24 * 3_600_000L;
        when(progressionConfig.getSeasonLengthMillis()).thenReturn(length);
        seasonRepository.saveSeasonState(
                new SeasonState("season-1", System.currentTimeMillis() - length - 5000));

        service.start();

        assertEquals("season-2", service.getCurrentSeason().get().seasonId(),
                "END_PENDING_ON_ENABLE must close a season that ended while offline");
    }

    @Test
    void endlessSeasonsAreNeverEndedByThePeriodicCheck() {
        when(progressionConfig.getSeasonLengthMillis()).thenReturn(0L);
        seasonRepository.saveSeasonState(new SeasonState("season-1", 0L));

        service.checkSeason();

        assertEquals("season-1", service.getCurrentSeason().get().seasonId(),
                "length 0 disables seasons entirely");
    }

    @Test
    void continueBehaviorKeepsAPendingSeasonOpenOnEnable() {
        long length = 30L * 24 * 3_600_000L;
        when(progressionConfig.getSeasonLengthMillis()).thenReturn(length);
        when(progressionConfig.getRestartBehavior())
                .thenReturn(AlignmentProgressionConfig.RestartBehavior.CONTINUE);
        seasonRepository.saveSeasonState(
                new SeasonState("season-1", System.currentTimeMillis() - length - 5000));

        service.start();

        assertEquals("season-1", service.getCurrentSeason().get().seasonId(),
                "CONTINUE must not close a season on enable; the periodic check owns it");
    }

    @Test
    void operationsFailGracefullyWhenSeasonStorageIsUnavailable() {
        seasonRepository.close();
        assertFalse(service.endSeason("any"));
        assertFalse(service.resetSeason());
        assertFalse(service.forceSnapshot());
        assertTrue(service.getSeasonAlignmentTotals().isEmpty());
        assertTrue(service.getCurrentSeason().isEmpty());
    }

    @Test
    @Disabled("BUG-QA4 (medium): /align leaderboard season reads player_season_reputation, "
            + "which is only written when a season ENDS - so the current-season leaderboard "
            + "is always empty during the season even though members have reputation. "
            + "Season views should fall back to live totals from the membership table.")
    void currentSeasonLeaderboardReflectsLiveReputation() {
        seasonRepository.saveSeasonState(new SeasonState("season-1", System.currentTimeMillis()));
        when(membershipRepository.getAlignmentTotals())
                .thenReturn(Map.of("azure_hearth", new long[] {420, 7}));
        when(membershipRepository.getAllMemberships()).thenReturn(List.of());

        assertFalse(service.getSeasonAlignmentTotals().isEmpty(),
                "the current season must expose live standings before it ends");
    }

    @Test
    @Disabled("BUG-QA6 (low): nextSeasonId() strips a fixed 7-character 'season-' prefix, "
            + "so a season named 'spring-2026' resets to 'season-2027' instead of "
            + "'spring-2026-next' - the custom season id is silently discarded. The id "
            + "suffix should only be incremented when it actually starts with 'season-'.")
    void nonNumericSeasonIdsGetSuffixedInsteadOfIncremented() {
        seasonRepository.saveSeasonState(new SeasonState("spring-2026", System.currentTimeMillis()));
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of());
        when(membershipRepository.getAllMemberships()).thenReturn(List.of());

        assertTrue(service.resetSeason());

        assertEquals("spring-2026-next", service.getCurrentSeason().get().seasonId(),
                "non-numeric season ids must be suffixed, not crash the increment");
    }
}