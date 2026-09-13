package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Alignment and campus parsing plus reference validation for contracts.yml. */
class ContractAlignmentConfigTest {
    @TempDir
    Path dataDirectory;

    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractAlignmentConfigTest"));
    }

    private ContractConfigManager managerWith(String contractsYml) throws Exception {
        Files.writeString(dataDirectory.resolve("contracts.yml"), contractsYml);
        ContractConfigManager manager = new ContractConfigManager(plugin);
        manager.load();
        return manager;
    }

    @Test
    void parsesAlignmentFieldsContractTypeAliasAndReputation() throws Exception {
        ContractConfigManager manager = managerWith("""
                contracts:
                  research_run:
                    contract_type: DELIVERY
                    display_name: "Research Courier"
                    description: "Courier research between campuses."
                    origin_campus: monash
                    destination_campus: sunway
                    origin_alignment: zenith_collective
                    required_alignment: zenith_collective
                    recommended_alignment: australis_sanctum
                    forbidden_alignment:
                      - spirewrights
                    reward_money: 400.0
                    reward_reputation: 7
                    start_endpoint: monash_campus_depot
                    end_endpoint: sunway_pyramid_depot
                    objective_description: "Reach the depot."
                """);

        ContractDefinition def = manager.getContract("research_run");
        assertEquals(ContractCategory.DELIVERY, def.category());
        assertEquals("Research Courier", def.name());
        assertEquals("monash", def.campusRoute().originCampus());
        assertEquals("sunway", def.campusRoute().destinationCampus());
        assertEquals("zenith_collective", def.campusRoute().originAlignment());
        assertEquals("zenith_collective", def.alignmentRule().requiredAlignment());
        assertEquals("australis_sanctum", def.alignmentRule().recommendedAlignment());
        assertTrue(def.alignmentRule().forbiddenAlignments().contains("spirewrights"));
        assertEquals(7, def.rewardReputation());
        assertTrue(def.enabled());
    }

    @Test
    void forbiddenAlignmentAsScalarBlocksOnlyThatAlignment() throws Exception {
        ContractConfigManager manager = managerWith("""
                contracts:
                  guarded:
                    category: HAULING
                    name: "Guarded haul"
                    description: "Hauling."
                    forbidden_alignment: spirewrights
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "Haul it."
                """);

        var rule = manager.getContract("guarded").alignmentRule();
        assertTrue(rule.canAccept("azure_hearth"));
        assertTrue(rule.canAccept(null));
        assertFalse(rule.canAccept("spirewrights"));
    }

    @Test
    void unknownAlignmentOrCampusReferencesDisableTheContractWithAReason() throws Exception {
        // alignments.yml present in the data folder makes alignment validation strict
        Files.writeString(dataDirectory.resolve("alignments.yml"), """
                alignments:
                  azure_hearth:
                    display_name: "Azure Hearth"
                """);
        ContractConfigManager manager = managerWith("""
                contracts:
                  bad_alignment:
                    category: HAULING
                    name: "Bad alignment"
                    description: "d"
                    required_alignment: not_an_alignment
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "d"
                  bad_campus:
                    category: HAULING
                    name: "Bad campus"
                    description: "d"
                    origin_campus: atlantis
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "d"
                """);

        assertTrue(manager.getContracts().isEmpty());
        assertTrue(manager.getDisabledReasons().containsKey("bad_alignment"));
        assertEquals("unknown alignment id: not_an_alignment",
                manager.getDisabledReasons().get("bad_alignment"));
        assertEquals("unknown campus id: atlantis",
                manager.getDisabledReasons().get("bad_campus"));
        assertNull(manager.getContract("bad_alignment"));
    }

    @Test
    void unknownContractTypeAndEnabledFalseEntriesLandInTheDisabledList() throws Exception {
        ContractConfigManager manager = managerWith("""
                contracts:
                  bad_type:
                    category: SABOTAGE
                    name: "Bad type"
                    description: "d"
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "d"
                  turned_off:
                    category: HAULING
                    name: "Turned off"
                    description: "d"
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "d"
                    enabled: false
                """);

        assertTrue(manager.getContracts().isEmpty());
        assertEquals("unknown contract type: SABOTAGE",
                manager.getDisabledReasons().get("bad_type"));
        assertEquals("disabled in contracts.yml",
                manager.getDisabledReasons().get("turned_off"));
        assertEquals(2, manager.getDisabledReasons().size());
    }

    @Test
    void withoutAlignmentsYmlAlignmentValidationStaysPermissive() throws Exception {
        ContractConfigManager manager = managerWith("""
                contracts:
                  speculative:
                    category: HAULING
                    name: "Speculative"
                    description: "d"
                    required_alignment: anything_goes
                    reward_money: 10.0
                    start_endpoint: a
                    end_endpoint: b
                    objective_description: "d"
                """);

        assertTrue(manager.getContracts().containsKey("speculative"),
                () -> "reasons=" + manager.getDisabledReasons());
        assertTrue(manager.getDisabledContracts().isEmpty());
    }
}
