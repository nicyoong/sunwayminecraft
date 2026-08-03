package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.config.SettingsConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractsManagerTest {
    private ContractConfigManager definitions;
    private SettingsConfigManager settings;
    private ContractPersistenceService persistence;
    private Economy economy;
    private Player player;
    private UUID playerId;
    private List<ActiveContract> active;
    private Map<String, Instant> cooldowns;
    private ContractsManager manager;

    @BeforeEach
    void setUp() {
        definitions = mock(ContractConfigManager.class);
        settings = mock(SettingsConfigManager.class);
        persistence = mock(ContractPersistenceService.class);
        economy = mock(Economy.class);
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        active = new ArrayList<>();
        cooldowns = new HashMap<>();
        manager = new ContractsManager(mock(JavaPlugin.class), definitions, mock(EndpointConfigManager.class), settings, persistence, economy);
        when(player.getUniqueId()).thenReturn(playerId);
        when(persistence.getPlayerContracts(playerId)).thenReturn(active);
        when(persistence.getPlayerCooldowns(playerId)).thenReturn(cooldowns);
        when(settings.getMaxActiveContracts()).thenReturn(3);
        when(definitions.getContract("contract")).thenReturn(definition("contract"));
    }

    @Test
    void cannotAcceptTheSameContractTwice() {
        assertTrue(manager.acceptContract(player, "contract"));
        assertFalse(manager.acceptContract(player, "contract"));
        assertEquals(1, active.size());
        verify(persistence, times(1)).save();
    }

    @Test
    void completionRequiresAnActiveCompletedNonExpiredContractOwnedByPlayer() {
        ActiveContract contract = new ActiveContract(playerId, "contract", Instant.now(), Instant.now().plusSeconds(60));
        active.add(contract);

        assertFalse(manager.completeContract(player, contract));
        contract.completeObjective();
        assertTrue(manager.completeContract(player, contract));
        verify(economy).depositPlayer(player, 50.0);
        assertTrue(active.isEmpty());
    }

    @Test
    void completionRejectsAContractOwnedBySomeoneElseWithoutPaying() {
        ActiveContract contract = new ActiveContract(UUID.randomUUID(), "contract", Instant.now(), Instant.now().plusSeconds(60));
        contract.completeObjective();
        active.add(contract);

        assertFalse(manager.completeContract(player, contract));
        verify(economy, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void cleanupExpiresContractsAppliesCooldownAndPersistsOnce() {
        ActiveContract expired = new ActiveContract(playerId, "contract", Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        ActiveContract current = new ActiveContract(playerId, "contract2", Instant.now(), Instant.now().plusSeconds(60));
        active.add(expired);
        active.add(current);
        when(persistence.getAllPlayerContracts()).thenReturn(Map.of(playerId, active));

        manager.cleanupExpiredContracts();

        assertEquals(List.of(current), active);
        assertTrue(cooldowns.get("contract").isAfter(Instant.now()));
        verify(persistence).save();
    }

    private ContractDefinition definition(String id) {
        return new ContractDefinition(id, ContractCategory.COURIER, "name", "description", 50, 30, 10,
                "start", "end", Map.of(), "objective");
    }
}
