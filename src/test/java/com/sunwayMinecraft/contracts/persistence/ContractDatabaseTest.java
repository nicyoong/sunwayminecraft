package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractDatabaseTest {
    @TempDir
    Path dataDirectory;

    private ContractDatabase database;

    @AfterEach
    void tearDown() {
        if (database != null) database.close();
    }

    private ContractDatabase open() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractDatabaseTest"));
        database = new ContractDatabase(plugin);
        return database;
    }

    @Test
    void addHasGetRemoveRoundTripsOneActivePerPlayerContract() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        ActiveContract contract = new ActiveContract(player, "stone", start, start.plusSeconds(60));
        contract.markStage("start");

        db.addActiveContract(contract);
        assertTrue(db.hasActiveContract(player, "stone"));
        assertEquals(1, db.countActiveContracts());

        ActiveContract loaded = db.getActiveContracts().get(0);
        assertEquals("stone", loaded.getContractId());
        assertTrue(loaded.hasStage("start"));
        assertEquals(start.toEpochMilli(), loaded.getStartTime().toEpochMilli());

        db.removeActiveContract(player, "stone");
        assertFalse(db.hasActiveContract(player, "stone"));
        assertEquals(0, db.countActiveContracts());
    }

    @Test
    void uniqueConstraintKeepsOneRowPerContractPerPlayer() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant start = Instant.now();
        for (int i = 0; i < 3; i++) {
            db.addActiveContract(new ActiveContract(player, "dupe", start, start.plusSeconds(60)));
        }
        assertEquals(1, db.countActiveContracts());
    }

    @Test
    void updateProgressStatePersistsStages() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant start = Instant.now();
        db.addActiveContract(new ActiveContract(player, "stone", start, start.plusSeconds(60)));
        db.updateProgressState(player, "stone", "start|mid");

        List<ActiveContract> rows = db.getActiveContracts();
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).hasStage("start"));
        assertTrue(rows.get(0).hasStage("mid"));
    }

    @Test
    void expireContractsFlipsOverdueRowsAndLeavesLiveOnes() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant now = Instant.now();
        db.addActiveContract(new ActiveContract(player, "old", now.minusSeconds(120), now.minusSeconds(1)));
        db.addActiveContract(new ActiveContract(player, "fresh", now, now.plusSeconds(120)));

        int expired = db.expireContracts(now.toEpochMilli());
        assertEquals(1, expired);
        assertEquals(1, db.countActiveContracts());
        assertTrue(db.hasActiveContract(player, "fresh"));
        assertFalse(db.hasActiveContract(player, "old"));
    }

    @Test
    void cooldownsRoundTripAndCompletionStatsAreStored() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        java.util.Map<UUID, java.util.Map<String, Instant>> cooldowns = new java.util.HashMap<>();
        cooldowns.put(player, new java.util.HashMap<>(java.util.Map.of("stone", Instant.parse("2026-02-01T00:00:00Z"))));
        db.saveCooldowns(cooldowns);

        assertEquals(Instant.parse("2026-02-01T00:00:00Z"),
                db.loadCooldowns().get(player).get("stone"));

        db.recordCompletion(player, "stone", "lagoon_covenant", "sunway",
                Instant.now(), 130.0, 5);
        // no direct count on stats; assert it does not throw and cooldowns survive a reopen
        db.close();
        ContractDatabase reopened = open();
        assertEquals(1, reopened.loadCooldowns().get(player).size());
    }
}
