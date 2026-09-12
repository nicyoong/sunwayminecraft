package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Campus, district, ownership and enabled metadata on contract endpoints. */
class ContractEndpointMetadataTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractEndpointMetadataTest"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void parsesAllianceMetadataAndHidesDisabledEndpoints() throws Exception {
        Files.writeString(dataDirectory.resolve("contract-endpoints.yml"), """
                endpoints:
                  lakeside_depot:
                    name: "Lakeside Depot"
                    type: DEPOT
                    world: world
                    x: 1.5
                    y: 64.0
                    z: 2.5
                    radius: 5.0
                    campus: TAYLORS
                    district: taylors_lakeside_campus
                    alignment_owner: Azure_Hearth
                    enabled: true
                  closed_gate:
                    name: "Closed Gate"
                    type: CAMPUS_GATE
                    world: world
                    x: 3.0
                    y: 64.0
                    z: 3.0
                    campus: sunway
                    enabled: false
                  plain:
                    name: "Plain Depot"
                    type: DEPOT
                    world: world
                    x: 4.0
                    y: 64.0
                    z: 4.0
                """);

        EndpointConfigManager manager = new EndpointConfigManager(plugin);
        manager.load();

        ContractEndpoint depot = manager.getEndpoint("lakeside_depot");
        assertNotNull(depot);
        assertEquals("taylors", depot.campus(), "metadata ids are normalized to lowercase");
        assertEquals("taylors_lakeside_campus", depot.districtId());
        assertEquals("azure_hearth", depot.alignmentOwner());
        assertEquals(5.0, depot.radius());

        ContractEndpoint gate = manager.getEndpoint("closed_gate");
        assertNull(gate, "disabled endpoints are hidden from objective lookups");
        assertNotNull(manager.getAllEndpoints().get("closed_gate"),
                "disabled endpoints remain visible for validation and admin listings");
        assertEquals(ContractEndpoint.EndpointType.CAMPUS_GATE,
                manager.getAllEndpoints().get("closed_gate").type());

        ContractEndpoint plain = manager.getEndpoint("plain");
        assertNull(plain.campus(), "legacy entries parse without alliance metadata");
        assertTrue(plain.enabled(), "endpoints default to enabled");
    }
}
