package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractVerificationServiceTest {
    private ServerMock server;
    private PlayerMock player;
    private ContractsManager manager;
    private ContractConfigManager contractConfig;
    private EndpointConfigManager endpointConfig;
    private ContractVerificationService verification;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("contracts");
        player = server.addPlayer();
        player.teleport(new Location(world, 10, 64, 10));

        manager = mock(ContractsManager.class);
        contractConfig = mock(ContractConfigManager.class);
        endpointConfig = mock(EndpointConfigManager.class);
        when(manager.getContractConfig()).thenReturn(contractConfig);
        when(manager.getEndpointConfig()).thenReturn(endpointConfig);
        verification = new ContractVerificationService(manager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void haulingAtDestinationConsumesExactlyTheRequiredMaterials() {
        ContractDefinition definition = definition(ContractCategory.HAULING, Map.of(Material.STONE, 64));
        ActiveContract active = active("haul", Instant.now().plusSeconds(300));
        when(contractConfig.getContract("haul")).thenReturn(definition);
        when(endpointConfig.getEndpoint("end")).thenReturn(endpointAt(player.getLocation(), 3));
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 32));
        player.getInventory().setItem(1, new ItemStack(Material.STONE, 32));
        player.getInventory().setItem(2, new ItemStack(Material.STONE, 5));

        ContractVerificationService.VerificationResult result = verification.verifyCompletion(player, active);

        assertTrue(result.success());
        assertEquals("Items delivered.", result.message());
        assertEquals(5, player.getInventory().all(Material.STONE).values().stream()
                .mapToInt(ItemStack::getAmount).sum());
    }

    @Test
    void haulingWithInsufficientMaterialsFailsWithoutRemovingItems() {
        ContractDefinition definition = definition(ContractCategory.HAULING, Map.of(Material.STONE, 64));
        when(contractConfig.getContract("haul")).thenReturn(definition);
        when(endpointConfig.getEndpoint("end")).thenReturn(endpointAt(player.getLocation(), 3));
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE, 63));

        ContractVerificationService.VerificationResult result = verification.verifyCompletion(player, active("haul", Instant.now().plusSeconds(300)));

        assertFalse(result.success());
        assertTrue(result.message().contains("Missing 64 STONE"));
        assertEquals(63, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void completionFailsWhenPlayerIsOutsideEndpointRadiusOrInAnotherWorld() {
        ContractDefinition definition = definition(ContractCategory.COURIER, Map.of());
        when(contractConfig.getContract("haul")).thenReturn(definition);
        when(endpointConfig.getEndpoint("end")).thenReturn(endpointAt(new Location(world, 100, 64, 100), 2));

        ContractVerificationService.VerificationResult result = verification.verifyCompletion(player, active("haul", Instant.now().plusSeconds(300)));

        assertFalse(result.success());
        assertTrue(result.message().contains("must be at"));
    }

    @Test
    void expiredContractIsFailedBeforeDestinationOrInventoryChecks() {
        when(contractConfig.getContract("haul")).thenReturn(definition(ContractCategory.COURIER, Map.of()));
        ActiveContract expired = active("haul", Instant.now().minusSeconds(1));

        ContractVerificationService.VerificationResult result = verification.verifyCompletion(player, expired);

        assertFalse(result.success());
        assertEquals("Contract has expired.", result.message());
        verify(manager).failContract(player, expired);
    }

    @Test
    void missingDefinitionOrEndpointProducesAnActionableFailure() {
        ActiveContract active = active("unknown", Instant.now().plusSeconds(300));

        ContractVerificationService.VerificationResult missingDefinition = verification.verifyCompletion(player, active);

        assertFalse(missingDefinition.success());
        assertEquals("Contract definition not found.", missingDefinition.message());

        ContractDefinition definition = new ContractDefinition("unknown", ContractCategory.COURIER, "Courier", "", 1,
                1, 1, "start", "end", Map.of(), "Go");
        when(contractConfig.getContract("unknown")).thenReturn(definition);
        ContractVerificationService.VerificationResult missingEndpoint = verification.verifyCompletion(player, active);

        assertFalse(missingEndpoint.success());
        assertEquals("Destination endpoint not found.", missingEndpoint.message());
    }

    private ContractDefinition definition(ContractCategory category, Map<Material, Integer> materials) {
        return new ContractDefinition("haul", category, "Contract", "", 100, 60, 30,
                "start", "end", materials, "Objective");
    }

    private ContractEndpoint endpointAt(Location location, double radius) {
        return new ContractEndpoint("end", "Depot", ContractEndpoint.EndpointType.DEPOT, location, radius);
    }

    private ActiveContract active(String id, Instant expiresAt) {
        return new ActiveContract(player.getUniqueId(), id, Instant.now().minusSeconds(10), expiresAt);
    }
}
