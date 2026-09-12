package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractSupplyServiceTest {
    @TempDir
    Path dataDirectory;

    private ContractDatabase database;
    private ContractSupplyService service;

    private static final Function<String, String> ALLIANCES = id -> id == null ? null
            : (id.equals("azure_hearth") || id.equals("lagoon_covenant")
                    ? "concordat_of_the_dawn" : "ironclad_syndicate");

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractSupplyServiceTest"));
        database = new ContractDatabase(plugin);
        service = new ContractSupplyService(database, ALLIANCES);
    }

    @AfterEach
    void tearDown() {
        if (database != null) database.close();
    }

    private ContractDefinition def(ContractCategory category) {
        return new ContractDefinition("c", category, "name", "d", 100, 30, 10, "s", "e",
                Map.of(), "obj", ContractObjectiveType.REACH_DESTINATION,
                new ContractAlignmentRule(null, null, List.of()), ContractCampusRoute.NONE, 0, true);
    }

    @Test
    void everyCategoryAwardsSupplyAndAccumulates() {
        assertEquals(1, service.recordCompletion(def(ContractCategory.COURIER), "azure_hearth"));
        assertEquals(3, service.recordCompletion(def(ContractCategory.HAULING), "azure_hearth"));
        assertEquals(3, service.recordCompletion(def(ContractCategory.EMERGENCY), "azure_hearth"));
        assertEquals(3, service.recordCompletion(def(ContractCategory.DIPLOMATIC), "azure_hearth"));
        assertEquals(10, service.getSupplyPoints("azure_hearth"));
    }

    @Test
    void unalignedCompletionAwardsNothing() {
        assertEquals(0, service.recordCompletion(def(ContractCategory.HAULING), null));
        assertEquals(0, service.getSupplyPoints(null));
    }

    @Test
    void allianceRollupSumsItsAlignments() {
        service.recordCompletion(def(ContractCategory.HAULING), "azure_hearth");   // 3
        service.recordCompletion(def(ContractCategory.COURIER), "lagoon_covenant"); // 1
        service.recordCompletion(def(ContractCategory.EMERGENCY), "zenith_collective"); // 3 (ironclad)
        assertEquals(4, service.getSupplyPointsForAlliance("concordat_of_the_dawn"));
        assertEquals(3, service.getSupplyPointsForAlliance("ironclad_syndicate"));
    }

    @Test
    void adminAdjustmentShiftsTotals() {
        service.adminAdjustSupply("azure_hearth", 50);
        service.adminAdjustSupply("azure_hearth", -10);
        assertEquals(40, service.getSupplyPoints("azure_hearth"));
    }
}
