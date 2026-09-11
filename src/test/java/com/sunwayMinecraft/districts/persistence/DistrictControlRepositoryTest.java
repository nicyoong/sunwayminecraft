package com.sunwayMinecraft.districts.persistence;

import com.sunwayMinecraft.districts.persistence.DistrictControlRepository.ControlHistoryRecord;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository.ControlStateRecord;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DistrictControlRepositoryTest {
    @TempDir
    Path dataDirectory;

    private final List<DistrictControlRepository> repositories = new ArrayList<>();

    @AfterEach
    void closeRepositories() {
        repositories.forEach(DistrictControlRepository::close);
        repositories.clear();
    }

    private DistrictControlRepository newRepository() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DistrictControlRepositoryTest"));
        DistrictControlRepository repository = new DistrictControlRepository(plugin);
        repositories.add(repository);
        return repository;
    }

    @Test
    void controlStateRoundTripsThroughSqlite() {
        DistrictControlRepository repository = newRepository();
        assertTrue(repository.isAvailable());
        assertTrue(repository.getState("pyramid").isEmpty());

        assertTrue(repository.saveState(new ControlStateRecord(
                "pyramid", "azure_hearth", "concordat_of_the_dawn",
                "capturing", "azure_hearth", 42, 1000L)));

        Optional<ControlStateRecord> state = repository.getState("pyramid");
        assertTrue(state.isPresent());
        assertEquals("azure_hearth", state.get().controllerAlignmentId());
        assertEquals("concordat_of_the_dawn", state.get().controllerGrandAllianceId());
        assertEquals("capturing", state.get().contestState());
        assertEquals(42, state.get().controlProgress());

        assertTrue(repository.saveState(new ControlStateRecord(
                "pyramid", "spirewrights", "ironclad_syndicate", "stable", null, 0, 2000L)));
        assertEquals("spirewrights", repository.getState("pyramid").get().controllerAlignmentId(),
                "saving replaces the existing row");
    }

    @Test
    void loadAllStatesReturnsEveryDistrict() {
        DistrictControlRepository repository = newRepository();
        repository.saveState(new ControlStateRecord(
                "a", "azure_hearth", "concordat_of_the_dawn", "stable", null, 0, 1L));
        repository.saveState(new ControlStateRecord(
                "b", null, null, "stable", null, 0, 1L));

        assertEquals(2, repository.loadAllStates().size());
    }

    @Test
    void historyAppendsAndReturnsNewestFirst() {
        DistrictControlRepository repository = newRepository();
        repository.appendHistory(new ControlHistoryRecord(
                0, "pyramid", null, "azure_hearth", "stable", 1000L, "captured"));
        repository.appendHistory(new ControlHistoryRecord(
                0, "pyramid", "azure_hearth", "spirewrights", "stable", 2000L, "captured"));
        repository.appendHistory(new ControlHistoryRecord(
                0, "other", null, null, "stable", 3000L, "neutral_reset"));

        List<ControlHistoryRecord> history = repository.getHistory("pyramid", 10);
        assertEquals(2, history.size());
        assertEquals("spirewrights", history.get(0).newAlignmentId(),
                "newest entry first");
        assertEquals("azure_hearth", history.get(1).newAlignmentId());

        assertEquals(1, repository.getHistory("pyramid", 1).size());
    }

    @Test
    void clearStateRemovesOnlyTheStateRow() {
        DistrictControlRepository repository = newRepository();
        repository.saveState(new ControlStateRecord(
                "pyramid", null, null, "stable", null, 0, 1L));
        repository.appendHistory(new ControlHistoryRecord(
                0, "pyramid", null, null, "stable", 1L, "kept"));

        assertTrue(repository.clearState("pyramid"));
        assertTrue(repository.getState("pyramid").isEmpty());
        assertEquals(1, repository.getHistory("pyramid", 10).size(),
                "history survives a state clear");
        assertFalse(repository.loadAllStates().containsKey("pyramid"));
    }

    @Test
    void closedRepositoryFailsGracefully() {
        DistrictControlRepository repository = newRepository();
        repository.close();
        assertFalse(repository.isAvailable());
        assertFalse(repository.saveState(new ControlStateRecord(
                "x", null, null, "stable", null, 0, 0L)));
        assertTrue(repository.loadAllStates().isEmpty());
        assertTrue(repository.getHistory("x", 5).isEmpty());
        assertFalse(repository.clearState("x"));
    }
}
