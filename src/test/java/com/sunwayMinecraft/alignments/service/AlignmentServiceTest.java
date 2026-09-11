package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignmentServiceTest {
    private AlignmentConfigManager configManager;
    private AlignmentRepository repository;
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
        repository = mock(AlignmentRepository.class);
        when(repository.isAvailable()).thenReturn(true);
        service = new AlignmentService(configManager, repository);
    }

    @Test
    void joinPersistsNewMembershipWhenAlignmentIsKnownAndEnabled() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(repository.upsert(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant"));

        verify(repository).upsert(argThat(membership ->
                membership.playerUuid().equals(playerUuid)
                        && membership.alignmentId().equals("lagoon_covenant")
                        && membership.status().equals("active")
                        && membership.reputation() == 0
                        && membership.joinedAt() > 0));
    }

    @Test
    void joinReplacesPreviousMembershipWithADifferentAlignment() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        AlignmentMembership old = new AlignmentMembership(
                playerUuid, "pyramid_ascendancy", 1000L, 25, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(old));
        when(repository.upsert(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        assertEquals(AlignmentResult.JOINED, service.join(playerUuid, "lagoon_covenant"));

        verify(repository).upsert(argThat(membership ->
                membership.alignmentId().equals("lagoon_covenant")
                        && membership.joinedAt() >= 1000L));
    }

    @Test
    void joinSameAlignmentIsRejectedWithoutRewrite() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 25, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));

        assertEquals(AlignmentResult.ALREADY_ALIGNED, service.join(playerUuid, "lagoon_covenant"));
        verify(repository, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void joinRejectsUnknownAndDisabledAlignmentsBeforeTouchingTheDatabase() {
        when(configManager.getAlignment("missing")).thenReturn(Optional.empty());
        when(configManager.getAlignment("retired_order")).thenReturn(Optional.of(retiredOrder));

        assertEquals(AlignmentResult.NOT_FOUND, service.join(playerUuid, "missing"));
        assertEquals(AlignmentResult.DISABLED, service.join(playerUuid, "retired_order"));
        verify(repository, never()).findByUuid(playerUuid);
        verify(repository, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void joinReportsDatabaseFailureWhenStorageIsUnavailableOrFails() {
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));

        when(repository.isAvailable()).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.join(playerUuid, "lagoon_covenant"));

        when(repository.isAvailable()).thenReturn(true);
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(repository.upsert(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.join(playerUuid, "lagoon_covenant"));
    }

    @Test
    void leaveRemovesMembershipAndReportsMissingOrDatabaseFailures() {
        when(repository.isAvailable()).thenReturn(true);
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        assertEquals(AlignmentResult.NOT_ALIGNED, service.leave(playerUuid));

        AlignmentMembership existing = new AlignmentMembership(
                playerUuid, "lagoon_covenant", 1000L, 0, "active");
        when(repository.findByUuid(playerUuid)).thenReturn(Optional.of(existing));
        when(repository.remove(playerUuid)).thenReturn(true);
        assertEquals(AlignmentResult.LEFT, service.leave(playerUuid));
        verify(repository).remove(playerUuid);

        when(repository.remove(playerUuid)).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.leave(playerUuid));

        when(repository.isAvailable()).thenReturn(false);
        assertEquals(AlignmentResult.DATABASE_FAILURE, service.leave(playerUuid));
    }

    @Test
    void availabilityTracksRepositoryState() {
        assertTrue(service.isAvailable());
        when(repository.isAvailable()).thenReturn(false);
        assertEquals(false, service.isAvailable());
    }
}
