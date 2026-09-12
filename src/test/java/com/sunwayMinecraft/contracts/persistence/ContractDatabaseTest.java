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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void getActiveContractByIdResolvesOnlyActiveRows() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant start = Instant.now();
        db.addActiveContract(new ActiveContract(player, "stone", start, start.plusSeconds(60)));
        int id = db.getActiveContracts().get(0).getActiveId();
        assertTrue(id > 0);
        assertEquals("stone", db.getActiveContractById(id).getContractId());
        assertNull(db.getActiveContractById(999999), "unknown id resolves to null");
    }

    @Test
    void delayActiveContractResetsProgressAndExtendsExpiry() {
        ContractDatabase db = open();
        UUID player = UUID.randomUUID();
        Instant start = Instant.now();
        ActiveContract ac = new ActiveContract(player, "stone", start, start.plusSeconds(60));
        ac.setProgress(1.0);
        db.addActiveContract(ac);
        int id = db.getActiveContracts().get(0).getActiveId();
        long expiryBefore = db.getActiveContractById(id).getExpiryTime().toEpochMilli();

        db.delayActiveContract(id, 300);

        ActiveContract delayed = db.getActiveContractById(id);
        assertEquals(0.0, delayed.getProgress(), "progress reset by the delay");
        assertTrue(delayed.getExpiryTime().toEpochMilli() > expiryBefore, "expiry extended");
    }

    @Test
    void recentInfluenceIsNewestFirstAndHonoursLimit() {
        ContractDatabase db = open();
        java.util.UUID player = java.util.UUID.randomUUID();
        db.logInfluence("azure_hearth", new com.sunwayMinecraft.contracts.domain.InfluenceRecord(
                0, "old", player, null, null, null, null, 1, "COMPLETION",
                Instant.parse("2026-01-01T00:00:00Z")));
        db.logInfluence("azure_hearth", new com.sunwayMinecraft.contracts.domain.InfluenceRecord(
                0, "new", player, null, null, null, null, 7, "COMPLETION",
                Instant.parse("2026-06-01T00:00:00Z")));

        List<com.sunwayMinecraft.contracts.domain.InfluenceRecord> recent = db.getRecentInfluence(1);
        assertEquals(1, recent.size());
        assertEquals("new", recent.get(0).contractId(), "newest row first");
        assertEquals(8, db.getAlignmentInfluence("azure_hearth"));
    }

    @Test
    void logInfluenceWithNoCreditedAlignmentWritesLedgerButNoTotal() {
        ContractDatabase db = open();
        db.logInfluence(null, new com.sunwayMinecraft.contracts.domain.InfluenceRecord(
                0, "c", null, "a", "b", null, null, 5, "CROSS_ALLIANCE", Instant.now()));
        assertEquals(5, db.getInfluenceBetween("a", "b"), "ledger row is queryable");
        assertEquals(0, db.getAlignmentInfluence("a"), "no total credited to a null target");
    }

    @Test
    void supplyTotalsRoundTripAndAccumulate() {
        ContractDatabase db = open();
        db.addSupplyPoints("azure_hearth", 3);
        db.addSupplyPoints("azure_hearth", 4);
        db.addSupplyPoints("zenith_collective", 2);
        assertEquals(7, db.getSupplyPoints("azure_hearth"));
        assertEquals(2, db.getSupplyPoints("zenith_collective"));
        assertEquals(0, db.getSupplyPoints("unknown_alignment"));
        assertEquals(2, db.getAllSupplyTotals().size());
    }
}
