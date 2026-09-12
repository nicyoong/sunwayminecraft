package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractPersistenceServiceTest {
    @TempDir
    Path dataDirectory;

    private final List<ContractPersistenceService> created = new ArrayList<>();

    // SQLite keeps the database file locked while open; @TempDir on Windows
    // cannot delete it until every service instance is closed
    @AfterEach
    void tearDown() {
        created.forEach(ContractPersistenceService::close);
    }

    private ContractPersistenceService serviceFor(JavaPlugin plugin) {
        ContractPersistenceService service = new ContractPersistenceService(plugin);
        created.add(service);
        return service;
    }

    @Test
    void savesAndRestoresContractsProgressAndCooldownsAcrossAServiceRestart() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiry = start.plusSeconds(3600);
        Instant cooldown = expiry.plusSeconds(600);

        ContractPersistenceService writer = serviceFor(plugin);
        ActiveContract active = new ActiveContract(playerId, "stone", start, expiry);
        active.setProgress(0.75);
        active.markStage("start");
        writer.getPlayerContracts(playerId).add(active);
        writer.getPlayerCooldowns(playerId).put("courier", cooldown);
        writer.save();

        ContractPersistenceService reader = serviceFor(plugin);

        assertEquals(1, reader.getPlayerContracts(playerId).size());
        ActiveContract restored = reader.getPlayerContracts(playerId).getFirst();
        assertEquals("stone", restored.getContractId());
        assertEquals(start, restored.getStartTime());
        assertEquals(expiry, restored.getExpiryTime());
        assertEquals(0.75, restored.getProgress());
        assertTrue(restored.hasStage("start"));
        assertEquals(cooldown, reader.getPlayerCooldowns(playerId).get("courier"));
    }

    @Test
    void removedContractsDisappearOnSave() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        UUID playerId = UUID.randomUUID();
        ContractPersistenceService service = serviceFor(plugin);
        ActiveContract active = new ActiveContract(playerId, "stone",
                Instant.now(), Instant.now().plusSeconds(600));
        service.getPlayerContracts(playerId).add(active);
        service.save();
        assertEquals(1, service.getDatabase().countActiveContracts());

        service.getPlayerContracts(playerId).remove(active);
        service.save();
        assertEquals(0, service.getDatabase().countActiveContracts());
        assertFalse(service.hasActiveContract(playerId, "stone"));
    }

    @Test
    void missingDataFileStartsWithEmptyMutableState() {
        ContractPersistenceService service = serviceFor(pluginFor(dataDirectory));
        UUID playerId = UUID.randomUUID();

        assertTrue(service.getPlayerContracts(playerId).isEmpty());
        assertTrue(service.getPlayerCooldowns(playerId).isEmpty());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractPersistenceServiceTest"));
        return plugin;
    }
}
