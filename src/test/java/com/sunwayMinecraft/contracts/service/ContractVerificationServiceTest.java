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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractVerificationServiceTest {
    @Test
    void reachDestinationMarksAPlayersActiveContractComplete() {
        Fixture fixture = new Fixture(ContractObjectiveType.REACH_DESTINATION, Map.of());

        ContractVerificationService.VerificationResult result = fixture.service.verifyCompletion(fixture.player, fixture.active);

        assertTrue(result.success());
        assertEquals("Destination reached.", result.message());
        assertTrue(fixture.active.isObjectiveComplete());
        verify(fixture.persistence).save();
    }

    @Test
    void interactionContractCannotCompleteUntilTheInteractionWasRecorded() {
        Fixture fixture = new Fixture(ContractObjectiveType.INTERACT_AT_DESTINATION, Map.of());

        ContractVerificationService.VerificationResult incomplete = fixture.service.verifyCompletion(fixture.player, fixture.active);
        assertFalse(incomplete.success());
        assertTrue(incomplete.message().contains("Interact"));

        fixture.active.completeObjective();
        assertTrue(fixture.service.verifyCompletion(fixture.player, fixture.active).success());
        verify(fixture.persistence, never()).save();
    }

    @Test
    void deliveryRequiresEveryMaterialBeforeChangingInventoryOrProgress() {
        Fixture fixture = new Fixture(ContractObjectiveType.DELIVER_MATERIALS, Map.of(Material.STONE, 4, Material.DIRT, 2));
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(fixture.player.getInventory()).thenReturn(inventory);
        when(inventory.containsAtLeast(any(ItemStack.class), anyInt())).thenAnswer(invocation ->
                invocation.getArgument(0, ItemStack.class).getType() == Material.STONE);

        ContractVerificationService.VerificationResult missing = fixture.service.verifyCompletion(fixture.player, fixture.active);

        assertFalse(missing.success());
        assertFalse(fixture.active.isObjectiveComplete());
        verify(inventory, never()).setItem(anyInt(), any());
        verify(fixture.persistence, never()).save();
    }

    @Test
    void deliveryConsumesRequiredItemsAndMarksObjectiveComplete() {
        Fixture fixture = new Fixture(ContractObjectiveType.DELIVER_MATERIALS, Map.of(Material.STONE, 4));
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack stack = new ItemStack(Material.STONE, 5);
        when(fixture.player.getInventory()).thenReturn(inventory);
        when(inventory.containsAtLeast(any(ItemStack.class), anyInt())).thenReturn(true);
        when(inventory.getContents()).thenReturn(new ItemStack[] { stack });

        assertTrue(fixture.service.verifyCompletion(fixture.player, fixture.active).success());
        assertEquals(1, stack.getAmount());
        assertTrue(fixture.active.isObjectiveComplete());
        verify(fixture.persistence).save();
    }

    @Test
    void rejectsContractsThatDoNotBelongToThePlayerOrAreNoLongerActive() {
        Fixture fixture = new Fixture(ContractObjectiveType.REACH_DESTINATION, Map.of());
        fixture.activeContracts.clear();

        ContractVerificationService.VerificationResult result = fixture.service.verifyCompletion(fixture.player, fixture.active);

        assertFalse(result.success());
        assertEquals("This contract is not active for you.", result.message());
        verify(fixture.persistence, never()).save();
    }

    private static final class Fixture {
        private final ContractsManager manager = mock(ContractsManager.class);
        private final ContractConfigManager definitions = mock(ContractConfigManager.class);
        private final EndpointConfigManager endpoints = mock(EndpointConfigManager.class);
        private final ContractPersistenceService persistence = mock(ContractPersistenceService.class);
        private final Player player = mock(Player.class);
        private final World world = mock(World.class);
        private final UUID playerId = UUID.randomUUID();
        private final ActiveContract active = new ActiveContract(playerId, "contract", Instant.now(), Instant.now().plusSeconds(60));
        private final List<ActiveContract> activeContracts = new ArrayList<>(List.of(active));
        private final ContractVerificationService service = new ContractVerificationService(manager);

        private Fixture(ContractObjectiveType type, Map<Material, Integer> materials) {
            when(manager.getContractConfig()).thenReturn(definitions);
            when(manager.getEndpointConfig()).thenReturn(endpoints);
            when(manager.getPersistence()).thenReturn(persistence);
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getLocation()).thenReturn(new Location(world, 10, 64, 10));
            when(persistence.getPlayerContracts(playerId)).thenReturn(activeContracts);
            when(definitions.getContract("contract")).thenReturn(new ContractDefinition("contract", ContractCategory.COURIER,
                    "name", "description", 50, 30, 10, "start", "end", materials, "objective", type));
            when(endpoints.getEndpoint("end")).thenReturn(new ContractEndpoint("end", "Destination", ContractEndpoint.EndpointType.DROPOFF,
                    new Location(world, 10, 64, 10), 2));
        }
    }
}
