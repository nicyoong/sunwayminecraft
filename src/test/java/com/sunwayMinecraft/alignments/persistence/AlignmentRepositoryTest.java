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
}
