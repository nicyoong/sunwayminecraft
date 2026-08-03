package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractObjectiveServiceTest {
    @Test
    void interactionCompletesOnlyEligibleObjectiveAtItsConfiguredEndpoint() {
        ContractsManager manager = mock(ContractsManager.class);
        ContractPersistenceService persistence = mock(ContractPersistenceService.class);
        ContractConfigManager definitions = mock(ContractConfigManager.class);
        EndpointConfigManager endpoints = mock(EndpointConfigManager.class);
        Player player = mock(Player.class);
        World world = mock(World.class);

        ActiveContract maintenance = new ActiveContract(java.util.UUID.randomUUID(), "lamp", Instant.now(), Instant.now().plusSeconds(60));
        ActiveContract courier = new ActiveContract(maintenance.getPlayerUuid(), "courier", Instant.now(), Instant.now().plusSeconds(60));
        List<ActiveContract> active = new ArrayList<>(List.of(maintenance, courier));
        when(player.getUniqueId()).thenReturn(maintenance.getPlayerUuid());
        when(manager.getPersistence()).thenReturn(persistence);
        when(manager.getContractConfig()).thenReturn(definitions);
        when(manager.getEndpointConfig()).thenReturn(endpoints);
        when(persistence.getPlayerContracts(maintenance.getPlayerUuid())).thenReturn(active);
        when(definitions.getContract("lamp")).thenReturn(definition("lamp", ContractObjectiveType.INTERACT_AT_DESTINATION));
        when(definitions.getContract("courier")).thenReturn(definition("courier", ContractObjectiveType.REACH_DESTINATION));
        when(endpoints.getEndpoint("end")).thenReturn(new ContractEndpoint("end", "Lamp", ContractEndpoint.EndpointType.MAINTENANCE_POINT,
                new Location(world, 10, 64, 10), 2));

        ContractObjectiveService service = new ContractObjectiveService(manager);

        assertEquals(1, service.recordInteraction(player, new Location(world, 11, 64, 10)));
        assertTrue(maintenance.isObjectiveComplete());
        assertFalse(courier.isObjectiveComplete());
        verify(persistence).save();
    }

    @Test
    void interactionIgnoresWrongLocationsExpiredAndAlreadyCompletedObjectives() {
        ContractsManager manager = mock(ContractsManager.class);
        ContractPersistenceService persistence = mock(ContractPersistenceService.class);
        ContractConfigManager definitions = mock(ContractConfigManager.class);
        EndpointConfigManager endpoints = mock(EndpointConfigManager.class);
        Player player = mock(Player.class);
        World world = mock(World.class);
        World otherWorld = mock(World.class);
        java.util.UUID uuid = java.util.UUID.randomUUID();
        ActiveContract expired = new ActiveContract(uuid, "expired", Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        ActiveContract complete = new ActiveContract(uuid, "complete", Instant.now(), Instant.now().plusSeconds(60));
        complete.completeObjective();
        when(player.getUniqueId()).thenReturn(uuid);
        when(manager.getPersistence()).thenReturn(persistence);
        when(manager.getContractConfig()).thenReturn(definitions);
        when(manager.getEndpointConfig()).thenReturn(endpoints);
        when(persistence.getPlayerContracts(uuid)).thenReturn(List.of(expired, complete));
        when(definitions.getContract(anyString())).thenReturn(definition("x", ContractObjectiveType.INTERACT_AT_DESTINATION));
        when(endpoints.getEndpoint("end")).thenReturn(new ContractEndpoint("end", "Lamp", ContractEndpoint.EndpointType.MAINTENANCE_POINT,
                new Location(world, 10, 64, 10), 2));

        ContractObjectiveService service = new ContractObjectiveService(manager);

        assertEquals(0, service.recordInteraction(player, new Location(otherWorld, 10, 64, 10)));
        assertEquals(0, service.recordInteraction(player, new Location(world, 20, 64, 10)));
        verify(persistence, never()).save();
    }

    @Test
    void interactionIgnoresCorruptEntriesOwnedByAnotherPlayer() {
        ContractsManager manager = mock(ContractsManager.class);
        ContractPersistenceService persistence = mock(ContractPersistenceService.class);
        Player player = mock(Player.class);
        java.util.UUID playerId = java.util.UUID.randomUUID();
        ActiveContract foreign = new ActiveContract(java.util.UUID.randomUUID(), "lamp", Instant.now(), Instant.now().plusSeconds(60));
        when(player.getUniqueId()).thenReturn(playerId);
        when(manager.getPersistence()).thenReturn(persistence);
        when(persistence.getPlayerContracts(playerId)).thenReturn(List.of(foreign));

        assertEquals(0, new ContractObjectiveService(manager).recordInteraction(player, mock(Location.class)));
        assertFalse(foreign.isObjectiveComplete());
        verify(persistence, never()).save();
    }

    private ContractDefinition definition(String id, ContractObjectiveType type) {
        return new ContractDefinition(id, ContractCategory.MAINTENANCE, id, "description", 25, 30, 10,
                "start", "end", Collections.emptyMap(), "objective", type);
    }
}
