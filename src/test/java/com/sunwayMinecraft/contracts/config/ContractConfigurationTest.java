package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractConfigurationTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void contractsLoadValidDefinitionsAndSkipInvalidDefinitionsOrMaterials() throws Exception {
        Files.writeString(dataDirectory.resolve("contracts.yml"), """
                contracts:
                  haul:
                    category: HAULING
                    name: Stone run
                    description: Deliver stone
                    reward_money: 55.5
                    duration_minutes: 45
                    cooldown_minutes: 12
                    start_endpoint: board
                    end_endpoint: depot
                    objective_description: Deliver
                    required_materials:
                      STONE: 64
                      NOT_A_MATERIAL: 1
                  invalid:
                    category: NOT_A_CATEGORY
                """);
        ContractConfigManager manager = new ContractConfigManager(pluginFor(dataDirectory));

        manager.load();

        ContractDefinition haul = manager.getContract("haul");
        assertNotNull(haul);
        assertEquals(ContractCategory.HAULING, haul.category());
        assertEquals(55.5, haul.rewardMoney());
        assertEquals(64, haul.requiredMaterials().get(org.bukkit.Material.STONE));
        assertEquals(1, haul.requiredMaterials().size());
        assertNull(manager.getContract("invalid"));
    }

    @Test
    void endpointAndSettingsConfigurationHonorWorldResolutionAndDefaults() throws Exception {
        Files.writeString(dataDirectory.resolve("contract-endpoints.yml"), """
                endpoints:
                  depot:
                    name: City depot
                    type: DEPOT
                    world: world
                    x: 1
                    y: 64
                    z: 2
                    radius: 5
                  missing-world:
                    name: Missing
                    type: BOARD
                    world: absent
                    x: 0
                    y: 0
                    z: 0
                """);
        Files.writeString(dataDirectory.resolve("contract-settings.yml"), "remote_accept_enabled: true\n");
        JavaPlugin plugin = pluginFor(dataDirectory);
        EndpointConfigManager endpoints = new EndpointConfigManager(plugin);
        SettingsConfigManager settings = new SettingsConfigManager(plugin);

        endpoints.load();
        settings.load();

        assertEquals(server.getWorld("world"), endpoints.getEndpoint("depot").location().getWorld());
        assertEquals(5, endpoints.getEndpoint("depot").radius());
        assertNull(endpoints.getEndpoint("missing-world").location().getWorld());
        assertEquals(3, settings.getMaxActiveContracts());
        assertTrue(settings.isRemoteAcceptEnabled());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractConfigurationTest"));
        return plugin;
    }
}
