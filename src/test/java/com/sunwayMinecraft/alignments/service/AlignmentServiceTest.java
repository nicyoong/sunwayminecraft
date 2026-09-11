package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignmentServiceTest {
    private AlignmentConfigManager configManager;
    private AlignmentSettingsConfig settings;
    private AlignmentRepository repository;
    private AlignmentCooldownManager cooldownManager;
    private AlignmentMembershipCache cache;
    private AlignmentPerkService perkService;
    private AlignmentService service;

    private final UUID playerUuid = UUID.randomUUID();
    private final AlignmentDefinition lagoonCovenant = new AlignmentDefinition(
            "lagoon_covenant", "Lagoon Covenant", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.SUNWAY, "desc", "[Lagoon Covenant]", "&3", true);
    private final AlignmentDefinition retiredOrder = new AlignmentDefinition(
            "retired_order", "Retired Order", GrandAlliance.IRONCLAD_SYNDICATE,
            Campus.MONASH, "desc", "[Retired Order]", "&8", false);

    @BeforeEach
    void setUp() {
        configManager = mock(AlignmentConfigManager.class);
        settings = mock(AlignmentSettingsConfig.class);
        repository = mock(AlignmentRepository.class);
        cooldownManager = mock(AlignmentCooldownManager.class);
        cache = mock(AlignmentMembershipCache.class);
        perkService = mock(AlignmentPerkService.class);
        when(perkService.applyCooldownReduction(any(UUID.class), anyLong()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(repository.isAvailable()).thenReturn(true);
        when(settings.getSwitchCooldownSeconds()).thenReturn(0L);
        when(settings.isCooldownAppliesToLeave()).thenReturn(false);
        when(cooldownManager.getRemainingSeconds(any(UUID.class), anyLong())).thenReturn(0L);
        service = new AlignmentService(
            configManager, settings, repository, cooldownManager, cache, perkService);
    }

    @Test
    void joinPersistsNewMembershipAndRefreshesCache() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant"));

        verify(repository).upsert(argThat(membership ->
                membership.playerUuid().equals(playerUuid)
                        && membership.alignmentId().equals("lagoon_covenant")
                        && membership.status().equals("active")
                        && membership.reputation() == 0
                        && membership.joinedAt() > 0));
        verify(cooldownManager).recordSwitch(playerUuid);
        verify(cache).update(eq(playerUuid), any(), eq(lagoonCovenant));
    }

    @Test
    void joinReplacesPreviousMembershipWithADifferentAlignment() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        AlignmentMembership old = new AlignmentMembership(
                playerUuid, "pyramid_ascendancy", 1000L, 25, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(old));
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant"));

        verify(repository).upsert(argThat(membership ->
                membership.alignmentId().equals("lagoon_covenant")
                        && membership.joinedAt() >= 1000L));
        verify(cache).update(eq(playerUuid), any(), eq(lagoonCovenant));
    }

    @Test
    void joinSameAlignmentIsRejectedWithoutRewrite() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 25, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));

        assertEquals(AlignmentResult.ALREADY_ALIGNED, service.join(playerUuid, "lagoon_covenant"));
        verify(repository, never()).upsert(any());
        verify(cooldownManager, never()).recordSwitch(playerUuid);
    }

    @Test
    void joinRejectsUnknownAndDisabledAlignmentsBeforeTouchingTheDatabase() {
        when(configManager.getAlignment("missing")).thenReturn(Optional.empty());
        when(configManager.getAlignment("retired_order")).thenReturn(Optional.of(retiredOrder));

        assertEquals(AlignmentResult.NOT_FOUND, service.join(playerUuid, "missing"));
        assertEquals(AlignmentResult.DISABLED, service.join(playerUuid, "retired_order"));
        verify(repository, never()).findByUuid(playerUuid);
        verify(repository, never()).upsert(any());
    }

    @Test
    void cooldownBlocksSwitchingUntilItExpires() {
        when(settings.getSwitchCooldownSeconds()).thenReturn(3600L);
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(cooldownManager.getRemainingSeconds(playerUuid, 3600L)).thenReturn(1200L);

        assertEquals(AlignmentResult.COOLDOWN_ACTIVE, service.join(playerUuid, "lagoon_covenant"));
        verify(repository, never()).upsert(any());
        assertEquals(1200L, service.getRemainingCooldownSeconds(playerUuid));

        when(cooldownManager.getRemainingSeconds(playerUuid, 3600L)).thenReturn(0L);
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(repository.upsert(any())).thenReturn(true);
        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant"));
    }

    @Test
    void adminJoinBypassesCooldown() {
        when(settings.getSwitchCooldownSeconds()).thenReturn(3600L);
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(cooldownManager.getRemainingSeconds(playerUuid, 3600L)).thenReturn(1200L);
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant", true));
        verify(cooldownManager, never()).getRemainingSeconds(any(UUID.class), anyLong());
        verify(cooldownManager).recordSwitch(playerUuid);
    }

    @Test
    void joinReportsDatabaseFailureWhenStorageIsUnavailableOrFails() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));

        when(repository.isAvailable()).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.join(playerUuid, "lagoon_covenant"));

        when(repository.isAvailable()).thenReturn(true);
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(repository.upsert(any())).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.join(playerUuid, "lagoon_covenant"));
        verify(cooldownManager, never()).recordSwitch(playerUuid);
    }

    @Test
    void leaveRemovesMembershipClearsCacheAndHonoursLeaveCooldownSetting() {
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 0, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.remove(playerUuid)).thenReturn(true);

        assertEquals(AlignmentResult.LEFT, service.leave(playerUuid));
        verify(cache).invalidate(playerUuid);
        verify(perkService).forgetPlayer(playerUuid);
        verify(cooldownManager, never()).recordSwitch(playerUuid);

        when(settings.isCooldownAppliesToLeave()).thenReturn(true);
        assertEquals(AlignmentResult.LEFT, service.leave(playerUuid));
        verify(cooldownManager).recordSwitch(playerUuid);
    }

    @Test
    void leaveReportsMissingOrDatabaseFailures() {
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_ALIGNED, service.leave(playerUuid));

        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 0, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.remove(playerUuid)).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.leave(playerUuid));

        when(repository.isAvailable()).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.leave(playerUuid));
    }

    @Test
    void adminSetForcesMembershipWithoutCooldownOrAlreadyAlignedCheck() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 25, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.adminSet(playerUuid, "lagoon_covenant"));
        verify(repository).upsert(any());
        verify(cache).update(eq(playerUuid), any(), eq(lagoonCovenant));
        verify(cooldownManager, never()).recordSwitch(any(UUID.class));

        when(configManager.getAlignment("missing")).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_FOUND, service.adminSet(playerUuid, "missing"));
    }

    @Test
    void adminClearRemovesMembershipAndCacheEntry() {
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_ALIGNED, service.adminClear(playerUuid));

        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 0, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.remove(playerUuid)).thenReturn(true);
        assertEquals(AlignmentResult.LEFT, service.adminClear(playerUuid));
        verify(cache).invalidate(playerUuid);
    }

    @Test
    void availabilityTracksRepositoryState() {
        assertTrue(service.isAvailable());
        when(repository.isAvailable()).thenReturn(false);
        assertFalse(service.isAvailable());
    }

    @Test
    void cooldownRemainingUsesThePerkReducedWindow() {
        when(settings.getSwitchCooldownSeconds()).thenReturn(100L);
        when(perkService.applyCooldownReduction(playerUuid, 100L)).thenReturn(75L);
        when(cooldownManager.getRemainingSeconds(playerUuid, 75L)).thenReturn(40L);

        assertEquals(40L, service.getRemainingCooldownSeconds(playerUuid));
    }

    @Test
    void adjustAndSetReputationUpdateTheDatabaseAndCache() {
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 50, "active");
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.adjustReputation(playerUuid, 25));
        verify(repository).upsert(argThat(m -> m.reputation() == 75 && m.joinedAt() == 1000L
                && m.status().equals("active")));
        verify(cache).update(eq(playerUuid), any(), eq(lagoonCovenant));

        assertEquals(AlignmentResult.JOINED, service.setReputation(playerUuid, 10));
        verify(repository).upsert(argThat(m -> m.reputation() == 10));

        // removal below zero is clamped, not wrapped
        assertEquals(AlignmentResult.JOINED, service.adjustReputation(playerUuid, -1000));
        verify(repository).upsert(argThat(m -> m.reputation() == 0));
    }

    @Test
    void reputationMutationsRequireAnActiveAlignmentAndAvailableStorage() {
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_ALIGNED, service.adjustReputation(playerUuid, 5));

        when(repository.isAvailable()).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.setReputation(playerUuid, 5));

        when(repository.isAvailable()).thenReturn(true);
        AlignmentMembership vanished = new AlignmentMembership(
                playerUuid, "vanished_alignment", 1000L, 5, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(vanished));
        when(configManager.getAlignment("vanished_alignment")).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_FOUND, service.adjustReputation(playerUuid, 5));
    }

    @Test
    void joinUnderCooldownRemainsIdempotentForTheSameAlignment() {
        when(settings.getSwitchCooldownSeconds()).thenReturn(3600L);
        when(cooldownManager.getRemainingSeconds(playerUuid, 3600L)).thenReturn(1200L);
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 5, "active")));

        assertEquals(AlignmentResult.ALREADY_ALIGNED, service.join(playerUuid, "lagoon_covenant"));
    }

    @Test
    void reputationAdditionMustNotOverflowToZero() {
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 2_000_000_000, "active");
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.upsert(any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.adjustReputation(playerUuid, 2_000_000_000));
        verify(repository).upsert(argThat(m -> m.reputation() >= 2_000_000_000));
    }
}