package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignmentCooldownManagerTest {
    private AlignmentRepository repository;
    private AlignmentCooldownManager manager;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(AlignmentRepository.class);
        manager = new AlignmentCooldownManager(repository);
    }

    @Test
    void disabledCooldownNeverQueriesTheDatabase() {
        assertEquals(0L, manager.getRemainingSeconds(playerUuid, 0L));
        verify(repository, never()).getLastSwitchAt(playerUuid);
    }

    @Test
    void recordSwitchPersistsAndStartsTheCooldown() {
        long before = System.currentTimeMillis();
        manager.recordSwitch(playerUuid);

        verify(repository).setLastSwitchAt(eq(playerUuid), anyLong());

        long remaining = manager.getRemainingSeconds(playerUuid, 60L);
        assertTrue(remaining > 55 && remaining <= 60,
                "fresh 60s cooldown should have almost the full window left, got " + remaining);
        assertTrue(System.currentTimeMillis() >= before);
    }

    @Test
    void missingMemoryEntryIsLoadedFromTheDatabase() {
        long thirtySecondsAgo = System.currentTimeMillis() - 30_000L;
        when(repository.getLastSwitchAt(playerUuid)).thenReturn(OptionalLong.of(thirtySecondsAgo));

        long remaining = manager.getRemainingSeconds(playerUuid, 60L);
        assertTrue(remaining >= 25 && remaining <= 35,
                "30s into a 60s cooldown should leave ~30s, got " + remaining);
    }

    @Test
    void expiredCooldownReportsZero() {
        long twoHoursAgo = System.currentTimeMillis() - 7_200_000L;
        when(repository.getLastSwitchAt(playerUuid)).thenReturn(OptionalLong.of(twoHoursAgo));

        assertEquals(0L, manager.getRemainingSeconds(playerUuid, 60L));
    }

    @Test
    void playersWithoutHistoryHaveNoCooldown() {
        when(repository.getLastSwitchAt(playerUuid)).thenReturn(OptionalLong.empty());
        assertEquals(0L, manager.getRemainingSeconds(playerUuid, 60L));
    }

    @Test
    void unavailableDatabaseIsTreatedAsNoCooldown() {
        when(repository.getLastSwitchAt(playerUuid)).thenReturn(OptionalLong.empty());
        assertEquals(0L, manager.getRemainingSeconds(playerUuid, 60L));
        verify(repository, never()).setLastSwitchAt(playerUuid, 0L);
    }
}
