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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
