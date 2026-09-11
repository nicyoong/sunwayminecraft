package com.sunwayMinecraft.alignments.service;

import org.bukkit.plugin.java.JavaPlugin;
import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import com.sunwayMinecraft.alignments.service.AlignmentRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignmentMembershipCacheTest {
    private AlignmentConfigManager configManager;
    private AlignmentRepository repository;
    private AlignmentSettingsConfig settings;
    private AlignmentMembershipCache cache;

    private final UUID playerUuid = UUID.randomUUID();
    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);
    private final AlignmentDefinition retiredOrder = new AlignmentDefinition(
            "retired_order", "Retired Order", GrandAlliance.IRONCLAD_SYNDICATE,
            Campus.MONASH, "desc", "[Retired Order]", "&8", false);

    @BeforeEach
    void setUp() {
        configManager = mock(AlignmentConfigManager.class);
        repository = mock(AlignmentRepository.class);
        settings = mock(AlignmentSettingsConfig.class);
        when(repository.isAvailable()).thenReturn(true);
        when(settings.getStaleMembershipAction()).thenReturn(
                AlignmentSettingsConfig.StaleMembershipAction.KEEP);
        cache = new AlignmentMembershipCache(
            configManager, repository,
            new AlignmentRankService(mock(AlignmentProgressionConfig.class)));
    }

    @Test
    void cacheMissLoadsFromSqliteAndDerivesAllianceAndCampus() {
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L)));

        Optional<AlignmentMembershipCache.CachedMembership> cached = cache.getOrLoad(playerUuid);
        assertTrue(cached.isPresent());
        assertEquals("azure_hearth", cached.get().alignmentId());
        assertEquals("concordat_of_the_dawn", cached.get().grandAllianceId());
        assertEquals("taylors", cached.get().campusId());

        // Second lookup must be served from the cache, not the database
        cache.getOrLoad(playerUuid);
        verify(repository, times(1)).findByUuid(playerUuid);
    }

    @Test
    void unknownCachedAlignmentIsReportedAndNotServed() {
        when(configManager.getAlignment("vanished")).thenReturn(Optional.empty());
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(
                AlignmentMembership.newMembership(playerUuid, "vanished", 1000L)));

        assertTrue(cache.getOrLoad(playerUuid).isEmpty());
    }

    @Test
    void updatePutsMembershipIntoTheCacheAndInvalidateRemovesIt() {
        cache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L), azureHearth);
        assertTrue(cache.get(playerUuid).isPresent());
        assertEquals("azure_hearth", cache.get(playerUuid).get().alignmentId());

        cache.invalidate(playerUuid);
        assertTrue(cache.get(playerUuid).isEmpty());
    }

    @Test
    void updateStatusChangesOnlyTheStatus() {
        cache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L), azureHearth);
        cache.updateStatus(playerUuid, "inactive");

        AlignmentMembershipCache.CachedMembership cached = cache.get(playerUuid).get();
        assertEquals("inactive", cached.status());
        assertEquals("azure_hearth", cached.alignmentId());
        assertEquals("concordat_of_the_dawn", cached.grandAllianceId());
    }

    @Test
    void revalidateKeepsStaleMembershipsMarkedInactiveWhenConfiguredToKeep() {
        cache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "retired_order", 1000L), retiredOrder);
        when(configManager.getAlignment("retired_order")).thenReturn(Optional.of(retiredOrder));

        cache.revalidate(settings);

        assertEquals("inactive", cache.get(playerUuid).get().status(),
                "KEEP policy must mark the membership inactive instead of removing it");
        verify(repository, never()).remove(playerUuid);
    }

    @Test
    void revalidateRemovesStaleMembershipsWhenConfiguredToUnalign() {
        when(settings.getStaleMembershipAction()).thenReturn(
                AlignmentSettingsConfig.StaleMembershipAction.UNALIGN);
        cache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "retired_order", 1000L), retiredOrder);
        when(configManager.getAlignment("retired_order")).thenReturn(Optional.of(retiredOrder));

        cache.revalidate(settings);

        assertTrue(cache.get(playerUuid).isEmpty());
        verify(repository).remove(playerUuid);
    }

    @Test
    void revalidateLeavesHealthyMembershipsUntouched() {
        cache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L), azureHearth);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));

        cache.revalidate(settings);

        assertEquals("active", cache.get(playerUuid).get().status());
        verify(repository, never()).remove(playerUuid);
    }

    @Test
    void updateDerivesTheRankFromTheProgressionConfig() throws Exception {
        // real progression config over the default resource gives steward at 150
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(java.nio.file.Files.createTempDirectory(
                "progression").toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("cache-rank"));
        org.mockito.Mockito.doAnswer(invocation -> {
            java.io.File out = new java.io.File(plugin.getDataFolder(),
                invocation.getArgument(0, String.class));
            out.getParentFile().mkdirs();
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
        AlignmentProgressionConfig progression = new AlignmentProgressionConfig(plugin);
        progression.load();
        AlignmentMembershipCache rankedCache = new AlignmentMembershipCache(
                configManager, repository, new AlignmentRankService(progression));
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));

        rankedCache.update(playerUuid,
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L), azureHearth);
        assertTrue(rankedCache.get(playerUuid).get().rankId() == null
                        || !rankedCache.get(playerUuid).get().rankId().equals("steward"),
                "0 reputation must not be steward");

        rankedCache.update(playerUuid,
                new AlignmentMembership(playerUuid, "azure_hearth", 1000L, 150, "active"),
                azureHearth);
        assertEquals("steward", rankedCache.get(playerUuid).get().rankId(),
                "150 reputation must resolve to the steward rank");
    }

}
