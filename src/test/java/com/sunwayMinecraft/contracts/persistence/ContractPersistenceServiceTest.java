package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractPersistenceServiceTest {
    @TempDir
    Path dataDirectory;

    @Test
    void savesAndRestoresContractsProgressAndCooldownsAcrossAServiceRestart() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        UUID playerId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiry = start.plusSeconds(3600);
        Instant cooldown = expiry.plusSeconds(600);

        ContractPersistenceService writer = new ContractPersistenceService(plugin);
        ActiveContract active = new ActiveContract(playerId, "stone", start, expiry);
        active.setProgress(0.75);
        writer.getPlayerContracts(playerId).add(active);
        writer.getPlayerCooldowns(playerId).put("courier", cooldown);
        writer.save();

        ContractPersistenceService reader = new ContractPersistenceService(plugin);

        assertEquals(1, reader.getPlayerContracts(playerId).size());
        ActiveContract restored = reader.getPlayerContracts(playerId).getFirst();
        assertEquals("stone", restored.getContractId());
        assertEquals(start, restored.getStartTime());
        assertEquals(expiry, restored.getExpiryTime());
        assertEquals(0.75, restored.getProgress());
        assertEquals(cooldown, reader.getPlayerCooldowns(playerId).get("courier"));
    }

    @Test
    void missingDataFileStartsWithEmptyMutableState() {
        ContractPersistenceService service = new ContractPersistenceService(pluginFor(dataDirectory));
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
