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
import com.sunwayMinecraft.events.service.EventModifierService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import static org.mockito.Mockito.*;

public class ContractsManagerTest {
    private ServerMock server;
    private JavaPlugin plugin;
    private ContractConfigManager contractConfig;
    private EndpointConfigManager endpointConfig;
    private SettingsConfigManager settingsConfig;
    private ContractPersistenceService persistence;
    private Economy economy;
    private ContractsManager manager;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        
        contractConfig = mock(ContractConfigManager.class);
        endpointConfig = mock(EndpointConfigManager.class);
        settingsConfig = mock(SettingsConfigManager.class);
        persistence = mock(ContractPersistenceService.class);
        economy = mock(Economy.class);
        
        when(settingsConfig.getMaxActiveContracts()).thenReturn(3);

        manager = new ContractsManager(plugin, contractConfig, endpointConfig, settingsConfig, persistence, economy);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testAcceptContract() {
        Player player = server.addPlayer();
        String contractId = "test_contract";
        ContractDefinition def = new ContractDefinition(
            contractId, ContractCategory.HAULING, "Name", "Desc", 100.0, 60, 30, "start", "end", Collections.emptyMap(), "Obj"
        );
        
        when(contractConfig.getContract(contractId)).thenReturn(def);
        when(persistence.getPlayerContracts(player.getUniqueId())).thenReturn(new ArrayList<>());
        when(persistence.getPlayerCooldowns(player.getUniqueId())).thenReturn(Collections.emptyMap());

        boolean accepted = manager.acceptContract(player, contractId);
        
        assertTrue(accepted);
        verify(persistence).save();
        assertEquals(1, persistence.getPlayerContracts(player.getUniqueId()).size());
    }

    @Test
    public void testAcceptContractRejectsUnknownContractLimitAndActiveCooldown() {
        Player player = server.addPlayer();
        when(persistence.getPlayerContracts(player.getUniqueId())).thenReturn(new ArrayList<>());
        when(persistence.getPlayerCooldowns(player.getUniqueId())).thenReturn(new HashMap<>());

        assertFalse(manager.acceptContract(player, "missing"));

        ContractDefinition definition = new ContractDefinition("test", ContractCategory.COURIER, "Name", "Desc",
                10, 60, 30, "start", "end", Collections.emptyMap(), "Obj");
        when(contractConfig.getContract("test")).thenReturn(definition);
        when(settingsConfig.getMaxActiveContracts()).thenReturn(0);
        assertFalse(manager.acceptContract(player, "test"));

        when(settingsConfig.getMaxActiveContracts()).thenReturn(3);
        Map<String, Instant> cooldowns = new HashMap<>();
        cooldowns.put("test", Instant.now().plusSeconds(60));
        when(persistence.getPlayerCooldowns(player.getUniqueId())).thenReturn(cooldowns);
        assertFalse(manager.acceptContract(player, "test"));
        verify(persistence, never()).save();
    }

    @Test
    public void testCompleteContractWithBoost() {
        Player player = server.addPlayer();
        String contractId = "test_contract";
        ContractDefinition def = new ContractDefinition(
            contractId, ContractCategory.HAULING, "Name", "Desc", 100.0, 60, 30, "start", "end", Collections.emptyMap(), "Obj"
        );
        
        ActiveContract ac = new ActiveContract(player.getUniqueId(), contractId, Instant.now(), Instant.now().plusSeconds(3600));
        
        when(contractConfig.getContract(contractId)).thenReturn(def);
        when(persistence.getPlayerContracts(player.getUniqueId())).thenReturn(new ArrayList<>(Collections.singletonList(ac)));
        
        EventModifierService eventModifier = mock(EventModifierService.class);
        when(eventModifier.getRewardMultiplier(ContractCategory.HAULING)).thenReturn(1.5);
        manager.setEventModifierService(eventModifier);

        boolean completed = manager.completeContract(player, ac);
        
        assertTrue(completed);
        verify(economy).depositPlayer(player, 150.0); // 100 * 1.5
        verify(persistence).save();
    }

    @Test
    public void testCompleteContractRejectsMissingDefinitionWithoutPayingOrPersisting() {
        Player player = server.addPlayer();
        ActiveContract active = new ActiveContract(player.getUniqueId(), "missing", Instant.now(), Instant.now().plusSeconds(60));

        assertFalse(manager.completeContract(player, active));

        verify(economy, never()).depositPlayer(any(Player.class), anyDouble());
        verify(persistence, never()).save();
    }

    @Test
    public void testAbandonContract() {
        Player player = server.addPlayer();
        String contractId = "test_contract";
        ContractDefinition def = new ContractDefinition(
            contractId, ContractCategory.HAULING, "Name", "Desc", 100.0, 60, 30, "start", "end", Collections.emptyMap(), "Obj"
        );
        
        ActiveContract ac = new ActiveContract(player.getUniqueId(), contractId, Instant.now(), Instant.now().plusSeconds(3600));
        
        when(contractConfig.getContract(contractId)).thenReturn(def);
        var active = new ArrayList<ActiveContract>();
        active.add(ac);
        when(persistence.getPlayerContracts(player.getUniqueId())).thenReturn(active);
        var cooldowns = new java.util.HashMap<String, Instant>();
        when(persistence.getPlayerCooldowns(player.getUniqueId())).thenReturn(cooldowns);

        manager.abandonContract(player, ac);
        
        assertTrue(active.isEmpty());
        assertNotNull(cooldowns.get(contractId));
        verify(persistence).save();
    }
}
