package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.config.ContractTemplateConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicContractServiceTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;
    private ContractDatabase database;
    private ContractConfigManager contractConfig;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DynamicContractServiceTest"));
    }

    @AfterEach
    void tearDown() {
        if (database != null) database.close();
        MockBukkit.unmock();
    }

    private DynamicContractService service(int cap) throws Exception {
        Files.writeString(dataDirectory.resolve("contract-endpoints.yml"), """
                endpoints:
                  city_depot:
                    name: "City Depot"
                    type: DEPOT
                    world: world
                    x: 0.0
                    y: 64.0
                    z: 0.0
                    radius: 5.0
                  depot_b:
                    name: "Depot B"
                    type: DEPOT
                    world: world
                    x: 10.0
                    y: 64.0
                    z: 10.0
                    radius: 5.0
                """);
        Files.writeString(dataDirectory.resolve("contract-templates.yml"), """
                templates:
                  supply_drop:
                    display_name: "Supply Drop"
                    contract_type: EMERGENCY
                    origin_campus: sunway
                    destination_campus: taylors
                    reward_money: 350.0
                    reward_reputation: 6
                    duration_minutes: 25
                    start_endpoint: city_depot
                    end_endpoint: depot_b
                    objective_description: "Reach depot_b"
                  broken_drop:
                    display_name: "Broken Drop"
                    contract_type: EMERGENCY
                    reward_money: 10.0
                    duration_minutes: 10
                    start_endpoint: city_depot
                    end_endpoint: does_not_exist
                    objective_description: "Reach nowhere"
                """);
        Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"),
                "max_active_dynamic_contracts: " + cap + "\n");

        EndpointConfigManager endpoints = new EndpointConfigManager(plugin);
        endpoints.load();
        ContractTemplateConfigManager templates = new ContractTemplateConfigManager(plugin);
        templates.load();
        contractConfig = new ContractConfigManager(plugin);
        contractConfig.load();
        database = new ContractDatabase(plugin);
        ContractDiplomacySettings settings = new ContractDiplomacySettings(plugin);
        settings.load();
        return new DynamicContractService(contractConfig, templates, endpoints, database, settings, plugin);
    }

    @Test
    void generateProducesAnEmergencyContractAndPersistsIt() throws Exception {
        DynamicContractService svc = service(5);
        Optional<String> id = svc.generate("supply_drop");
        assertTrue(id.isPresent());
        assertTrue(id.get().startsWith("dyn_supply_drop_"));
        ContractDefinition generated = contractConfig.getContract(id.get());
        assertNotNull(generated);
        assertEquals(ContractCategory.EMERGENCY, generated.category());
        assertTrue(svc.liveIds().contains(id.get()));

        // a fresh service rebuilds the same contract from SQLite
        contractConfig.removeRuntimeContract(id.get());
        DynamicContractService reopened = new DynamicContractService(contractConfig, templates(),
                endpoints(), database, settings(), plugin);
        reopened.loadPersisted();
        assertTrue(reopened.liveIds().contains(id.get()));
        assertNotNull(contractConfig.getContract(id.get()));
    }

    @Test
    void loadPersistedRebuildsLiveAndSkipsExpired() throws Exception {
        DynamicContractService svc = service(5);
        Optional<String> live = svc.generate("supply_drop");
        assertTrue(live.isPresent());
        // inject a hard-expired row directly
        database.saveDynamicContract("dyn_stale", "supply_drop", Instant.now().minusSeconds(60).toEpochMilli());

        // fresh view of the world: clear runtime, rebuild from SQLite
        contractConfig.removeRuntimeContract(live.get());
        DynamicContractService rebuilt = new DynamicContractService(contractConfig, templates(),
                endpoints(), database, settings(), plugin);
        rebuilt.loadPersisted();

        assertTrue(rebuilt.liveIds().contains(live.get()), "non-expired row is rebuilt");
        assertTrue(!rebuilt.liveIds().contains("dyn_stale"), "expired row is not loaded");
        assertNotNull(contractConfig.getContract(live.get()));
    }

    @Test
    void capRefusesGenerationBeyondTheLimit() throws Exception {
        DynamicContractService svc = service(1);
        assertTrue(svc.generate("supply_drop").isPresent());
        assertTrue(svc.generate("supply_drop").isEmpty(), "cap of one reached");
    }

    @Test
    void templateWithMissingEndpointIsNotGenerated() throws Exception {
        DynamicContractService svc = service(5);
        assertTrue(svc.generate("broken_drop").isEmpty());
        assertTrue(svc.liveIds().isEmpty());
    }

    @Test
    void unknownTemplateYieldsEmpty() throws Exception {
        DynamicContractService svc = service(5);
        assertTrue(svc.generate("nope").isEmpty());
    }

    @Test
    void clearAllRemovesEveryDynamicContract() throws Exception {
        DynamicContractService svc = service(5);
        String a = svc.generate("supply_drop").orElseThrow();
        String b = svc.generate("supply_drop").orElseThrow();
        assertEquals(2, svc.liveIds().size());
        assertEquals(2, svc.clearAll());
        assertTrue(svc.liveIds().isEmpty());
        org.junit.jupiter.api.Assertions.assertNull(contractConfig.getContract(a));
        org.junit.jupiter.api.Assertions.assertNull(contractConfig.getContract(b));
    }

    // --- small loaders reused across service instances --------------------

    private EndpointConfigManager endpoints() {
        EndpointConfigManager endpoints = new EndpointConfigManager(plugin);
        endpoints.load();
        return endpoints;
    }

    private ContractTemplateConfigManager templates() {
        ContractTemplateConfigManager templates = new ContractTemplateConfigManager(plugin);
        templates.load();
        return templates;
    }

    private ContractDiplomacySettings settings() {
        ContractDiplomacySettings settings = new ContractDiplomacySettings(plugin);
        settings.load();
        return settings;
    }
}
