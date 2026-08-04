package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.config.SettingsConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.events.service.EventModifierService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
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
    void acceptsKnownContractAndRejectsDuplicatesUnknownContractsLimitsAndCooldowns() {
        assertTrue(manager.acceptContract(player, "contract"));
        assertFalse(manager.acceptContract(player, "contract"));
        assertFalse(manager.acceptContract(player, "missing"));
        assertEquals(1, active.size());

        active.clear();
        when(settings.getMaxActiveContracts()).thenReturn(0);
        assertFalse(manager.acceptContract(player, "contract"));

        when(settings.getMaxActiveContracts()).thenReturn(3);
        cooldowns.put("contract", Instant.now().plusSeconds(60));
        assertFalse(manager.acceptContract(player, "contract"));
        verify(persistence, times(1)).save();
    }

    @Test
    void completionRequiresAnActiveCompletedNonExpiredContractOwnedByPlayer() {
        ActiveContract contract = activeContract("contract", playerId);
        active.add(contract);

        assertFalse(manager.completeContract(player, contract));
        contract.completeObjective();
        assertTrue(manager.completeContract(player, contract));
        verify(economy).depositPlayer(player, 50.0);
        assertTrue(active.isEmpty());
    }

    @Test
    void completionUsesEventRewardMultiplierAfterObjectiveIsCompleted() {
        ActiveContract contract = activeContract("contract", playerId);
        contract.completeObjective();
        active.add(contract);
        EventModifierService eventModifier = mock(EventModifierService.class);
        when(eventModifier.getRewardMultiplier(ContractCategory.COURIER)).thenReturn(1.5);
        manager.setEventModifierService(eventModifier);

        assertTrue(manager.completeContract(player, contract));

        verify(economy).depositPlayer(player, 75.0);
    }

    @Test
    void completionRejectsForeignOrUndefinedContractsWithoutPaying() {
        ActiveContract foreign = activeContract("contract", UUID.randomUUID());
        foreign.completeObjective();
        active.add(foreign);
        assertFalse(manager.completeContract(player, foreign));

        active.clear();
        ActiveContract missing = activeContract("missing", playerId);
        missing.completeObjective();
        active.add(missing);
        assertFalse(manager.completeContract(player, missing));
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(persistence, never()).save();
    }

    @Test
    void abandoningAppliesCooldownAndRemovesTheContract() {
        ActiveContract contract = activeContract("contract", playerId);
        active.add(contract);

        manager.abandonContract(player, contract);

        assertTrue(active.isEmpty());
        assertTrue(cooldowns.get("contract").isAfter(Instant.now()));
        verify(persistence).save();
    }

    @Test
    void cleanupExpiresContractsAppliesCooldownAndPersistsOnce() {
        ActiveContract expired = new ActiveContract(playerId, "contract", Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        ActiveContract current = activeContract("contract2", playerId);
        active.add(expired);
        active.add(current);
        when(persistence.getAllPlayerContracts()).thenReturn(Map.of(playerId, active));

        manager.cleanupExpiredContracts();

        assertEquals(List.of(current), active);
        assertTrue(cooldowns.get("contract").isAfter(Instant.now()));
        verify(persistence).save();
    }

    private ActiveContract activeContract(String id, UUID owner) {
        return new ActiveContract(owner, id, Instant.now(), Instant.now().plusSeconds(60));
    }

    private ContractDefinition definition(String id) {
        return new ContractDefinition(id, ContractCategory.COURIER, "name", "description", 50, 30, 10,
                "start", "end", Map.of(), "objective");
    }
}
