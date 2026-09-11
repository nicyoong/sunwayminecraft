package com.sunwayMinecraft.alignments.persistence;

import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentRepositoryTest {
    @TempDir
    Path dataDirectory;

    private final List<AlignmentRepository> repositories = new ArrayList<>();

    @AfterEach
    void closeRepositories() {
        repositories.forEach(AlignmentRepository::close);
        repositories.clear();
    }

    private AlignmentRepository newRepository() {
        AlignmentRepository repository = new AlignmentRepository(pluginFor(dataDirectory));
        repositories.add(repository);
        return repository;
    }

    @Test
    void upsertFindAndRemoveRoundTripThroughSqlite() {
        UUID playerUuid = UUID.randomUUID();
        AlignmentRepository repository = newRepository();
        assertTrue(repository.isAvailable());

        assertTrue(repository.upsert(
                AlignmentMembership.newMembership(playerUuid, "azure_hearth", 1000L)));

        Optional<AlignmentMembership> found = repository.findByUuid(playerUuid);
        assertTrue(found.isPresent());
        assertEquals("azure_hearth", found.get().alignmentId());
        assertEquals(1000L, found.get().joinedAt());
        assertEquals(0, found.get().reputation());
        assertEquals("active", found.get().status());

        assertTrue(repository.upsert(new AlignmentMembership(
                playerUuid, "zenith_collective", 2000L, 15, "active")));
        Optional<AlignmentMembership> replaced = repository.findByUuid(playerUuid);
        assertTrue(replaced.isPresent());
        assertEquals("zenith_collective", replaced.get().alignmentId());
        assertEquals(15, replaced.get().reputation());

        assertTrue(repository.remove(playerUuid));
        assertTrue(repository.findByUuid(playerUuid).isEmpty());
    }

    @Test
    void unknownPlayersAndClosedRepositoriesReportEmptyOrUnavailable() {
        AlignmentRepository repository = newRepository();

        assertEquals(Optional.empty(), repository.findByUuid(UUID.randomUUID()));

        repository.close();
        assertFalse(repository.isAvailable());
        assertFalse(repository.upsert(
                AlignmentMembership.newMembership(UUID.randomUUID(), "azure_hearth", 1000L)));
        assertFalse(repository.remove(UUID.randomUUID()));
        assertEquals(Optional.empty(), repository.findByUuid(UUID.randomUUID()));
    }

    @Test
    void membershipsPersistAcrossRepositoryInstances() {
        UUID playerUuid = UUID.randomUUID();
        AlignmentRepository writer = newRepository();
        writer.upsert(AlignmentMembership.newMembership(playerUuid, "spirewrights", 3000L));
        writer.close();

        AlignmentRepository reader = newRepository();
        Optional<AlignmentMembership> found = reader.findByUuid(playerUuid);
        assertTrue(found.isPresent());
        assertEquals("spirewrights", found.get().alignmentId());
        reader.close();
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentRepositoryTest"));
        return plugin;
    }

    @Test
    void updateReputationReportsFalseForUnknownPlayers() {
        AlignmentRepository repository = newRepository();
        assertFalse(repository.updateReputation(UUID.randomUUID(), 100));
    }

    @Test
    void reputationPositionCountsOnlyActiveHigherReputations() {
        AlignmentRepository repository = newRepository();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        repository.upsert(new AlignmentMembership(first, "azure_hearth", 1000L, 300, "active"));
        repository.upsert(new AlignmentMembership(second, "azure_hearth", 1000L, 100, "active"));
        repository.upsert(new AlignmentMembership(inactive, "azure_hearth", 1000L, 900, "inactive"));
        repository.upsert(new AlignmentMembership(third, "spirewrights", 1000L, 999, "active"));

        assertEquals(1, repository.getReputationPosition("azure_hearth", 300),
                "the top active member is ranked #1");
        assertEquals(2, repository.getReputationPosition("azure_hearth", 100),
                "inactive higher reputation must not count towards standing");
        assertEquals(3, repository.getReputationPosition("azure_hearth", 0),
                "active members of other alignments must not count");
        assertEquals(1, repository.getReputationPosition("unknown_alignment", 10),
                "documented quirk: unknown alignments report the would-be rank 1; "
                    + "unreachable via commands because the caller checks membership first");
    }

    @Test
    void cooldownTableRoundTripsThroughSqlite() {
        AlignmentRepository repository = newRepository();
        UUID playerUuid = UUID.randomUUID();

        assertTrue(repository.getLastSwitchAt(playerUuid).isEmpty());
        assertTrue(repository.setLastSwitchAt(playerUuid, 5000L));
        assertEquals(5000L, repository.getLastSwitchAt(playerUuid).getAsLong());
        assertTrue(repository.setLastSwitchAt(playerUuid, 6000L),
                "a second switch must replace the timestamp");
        assertEquals(6000L, repository.getLastSwitchAt(playerUuid).getAsLong());
    }

    @Test
    void totalsAndBulkReadsReflectStoredMemberships() {
        AlignmentRepository repository = newRepository();
        UUID aligned = UUID.randomUUID();
        UUID unaligned = UUID.randomUUID();
        repository.upsert(new AlignmentMembership(aligned, "azure_hearth", 1000L, 120, "active"));
        repository.upsert(new AlignmentMembership(UUID.randomUUID(), "azure_hearth", 1000L, 30, "active"));
        repository.upsert(new AlignmentMembership(UUID.randomUUID(), "spirewrights", 1000L, 10, "active"));

        var totals = repository.getAlignmentTotals();
        assertEquals(2, totals.size());
        assertEquals(150L, totals.get("azure_hearth")[0]);
        assertEquals(2L, totals.get("azure_hearth")[1]);
        assertEquals(10L, totals.get("spirewrights")[0]);

        assertEquals(3, repository.getAllMemberships().size());
        var counts = repository.countByAlignment();
        assertEquals(2, counts.get("azure_hearth"));
        assertEquals(1, counts.get("spirewrights"));

        // memberships added after the totals snapshot appear on the next read
        repository.upsert(AlignmentMembership.newMembership(unaligned, "lagoon_covenant", 2000L));
        assertEquals(4, repository.getAllMemberships().size());
    }
}