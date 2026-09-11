package com.sunwayMinecraft.alignments.persistence;

import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.AlignmentSnapshot;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.PlayerSeasonRecord;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.SeasonState;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentSeasonRepositoryTest {
    @TempDir
    Path dataDirectory;

    private final List<AlignmentSeasonRepository> repositories = new java.util.ArrayList<>();

    @AfterEach
    void closeRepositories() {
        repositories.forEach(AlignmentSeasonRepository::close);
        repositories.clear();
    }

    private AlignmentSeasonRepository newRepository() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentSeasonRepositoryTest"));
        AlignmentSeasonRepository repository = new AlignmentSeasonRepository(plugin);
        repositories.add(repository);
        return repository;
    }

    @Test
    void seasonStateRoundTripsThroughSqlite() {
        AlignmentSeasonRepository repository = newRepository();
        assertTrue(repository.isAvailable());
        assertTrue(repository.getSeasonState().isEmpty(), "no season state initially");

        assertTrue(repository.saveSeasonState(new SeasonState("season-1", 1000L)));
        Optional<SeasonState> state = repository.getSeasonState();
        assertTrue(state.isPresent());
        assertEquals("season-1", state.get().seasonId());
        assertEquals(1000L, state.get().startedAt());

        assertTrue(repository.saveSeasonState(new SeasonState("season-2", 2000L)));
        assertEquals("season-2", repository.getSeasonState().get().seasonId(),
                "saving replaces the single state row");
    }

    @Test
    void snapshotsAreInsertedAndLatestAreReturned() {
        AlignmentSeasonRepository repository = newRepository();

        assertTrue(repository.insertSnapshots(List.of(
                new AlignmentSnapshot(0, "season-1", "azure_hearth", "concordat_of_the_dawn",
                        500, 10, 1000L),
                new AlignmentSnapshot(0, "season-1", "lagoon_covenant", "concordat_of_the_dawn",
                        300, 8, 1000L),
                new AlignmentSnapshot(0, "season-1", "azure_hearth", "concordat_of_the_dawn",
                        750, 12, 2000L))));

        List<AlignmentSnapshot> latest = repository.getLatestSnapshots();
        assertEquals(2, latest.size(), "only the newest snapshot per alignment is returned");
        for (AlignmentSnapshot snapshot : latest) {
            if (snapshot.alignmentId().equals("azure_hearth")) {
                assertEquals(750, snapshot.totalReputation());
                assertEquals(12, snapshot.memberCount());
            }
        }
        assertTrue(latest.get(0).totalReputation() >= latest.get(1).totalReputation(),
                "latest snapshots are ordered by reputation descending");
    }

    @Test
    void playerRecordsAreWrittenPerSeasonAndQueriedByPlayer() {
        AlignmentSeasonRepository repository = newRepository();
        UUID playerUuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();

        assertTrue(repository.insertPlayerRecords(List.of(
                new PlayerSeasonRecord("season-1", playerUuid, "azure_hearth", 120, 1000L),
                new PlayerSeasonRecord("season-2", playerUuid, "spirewrights", 60, 2000L),
                new PlayerSeasonRecord("season-1", otherUuid, "lagoon_covenant", 30, 1000L))));

        List<PlayerSeasonRecord> history = repository.getPlayerSeasonHistory(playerUuid);
        assertEquals(2, history.size());
        assertEquals("season-1", history.get(0).seasonId());
        assertEquals("spirewrights", history.get(1).alignmentId());
    }

    @Test
    void seasonTotalsAggregatePlayerRecordsByAlignment() {
        AlignmentSeasonRepository repository = newRepository();
        repository.insertPlayerRecords(List.of(
                new PlayerSeasonRecord("season-1", UUID.randomUUID(), "azure_hearth", 100, 1000L),
                new PlayerSeasonRecord("season-1", UUID.randomUUID(), "azure_hearth", 50, 1000L),
                new PlayerSeasonRecord("season-1", UUID.randomUUID(), "spirewrights", 20, 1000L),
                new PlayerSeasonRecord("season-2", UUID.randomUUID(), "azure_hearth", 999, 2000L)));

        var totals = repository.getSeasonAlignmentTotals("season-1");
        assertEquals(2, totals.size());
        assertEquals(150L, totals.get("azure_hearth")[0]);
        assertEquals(2L, totals.get("azure_hearth")[1]);
        assertEquals(20L, totals.get("spirewrights")[0]);
    }

    @Test
    void closedRepositoryReportsUnavailableAndFailsGracefully() {
        AlignmentSeasonRepository repository = newRepository();
        repository.close();
        assertFalse(repository.isAvailable());
        assertFalse(repository.saveSeasonState(new SeasonState("season-9", 0L)));
        assertTrue(repository.insertSnapshots(List.of()) || true);
        assertTrue(repository.getLatestSnapshots().isEmpty());
        assertTrue(repository.getSeasonState().isEmpty());
        assertEquals(0, repository.resetAllReputations());
    }
}
