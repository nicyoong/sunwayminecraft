package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractTemplateConfigManagerTest {
    @TempDir
    Path dataDirectory;

    private ContractTemplateConfigManager load(String templatesYml) throws Exception {
        Files.writeString(dataDirectory.resolve("contract-templates.yml"), templatesYml);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractTemplateConfigManagerTest"));
        ContractTemplateConfigManager manager = new ContractTemplateConfigManager(plugin);
        manager.load();
        return manager;
    }

    @Test
    void parsesFieldsDefaultsAndLowercaseLookup() throws Exception {
        ContractTemplateConfigManager manager = load("""
                templates:
                  Supply_Drop:
                    display_name: "Supply Drop"
                    reward_money: 300.0
                    duration_minutes: 18
                    start_endpoint: city_depot
                    end_endpoint: depot_b
                """);
        // no contract_type -> defaults to EMERGENCY, and default objective for EMERGENCY
        ContractDefinition def = manager.getTemplate("supply_drop"); // case-insensitive lookup
        assertNotNull(def);
        assertEquals(ContractCategory.EMERGENCY, def.category());
        assertEquals(ContractObjectiveType.REACH_DESTINATION, def.objectiveType());
        assertEquals(300.0, def.rewardMoney());
        assertEquals(18, def.durationMinutes());
    }

    @Test
    void forbiddenAlignmentListAndRequiredAreParsedLowercased() throws Exception {
        ContractTemplateConfigManager manager = load("""
                templates:
                  t:
                    category: ESCORT
                    required_alignment: AZURE_HEARTH
                    forbidden_alignment:
                      - SpireWrights
                      - zenith_collective
                    start_endpoint: a
                    end_endpoint: b
                """);
        var rule = manager.getTemplate("t").alignmentRule();
        assertEquals("azure_hearth", rule.requiredAlignment());
        assertTrue(rule.forbiddenAlignments().contains("spirewrights"));
        assertTrue(rule.forbiddenAlignments().contains("zenith_collective"));
    }

    @Test
    void malformedTemplateIsSkippedWithoutCrashing() throws Exception {
        ContractTemplateConfigManager manager = load("""
                templates:
                  broken:
                    category: SABOTAGE_MODE
                    start_endpoint: a
                    end_endpoint: b
                  good:
                    category: HAULING
                    start_endpoint: a
                    end_endpoint: b
                """);
        assertNull(manager.getTemplate("broken"), "unknown category template is skipped");
        assertNotNull(manager.getTemplate("good"), "sibling templates still load");
    }

    @Test
    void requiredMaterialsAreParsedAndUnknownMaterialsIgnored() throws Exception {
        ContractTemplateConfigManager manager = load("""
                templates:
                  t:
                    category: HAULING
                    objective_type: DELIVER_MATERIALS
                    start_endpoint: a
                    end_endpoint: b
                    required_materials:
                      STONE: 32
                      NOT_A_REAL_ITEM: 5
                """);
        ContractDefinition def = manager.getTemplate("t");
        assertEquals(32, def.requiredMaterials().get(Material.STONE));
        assertEquals(1, def.requiredMaterials().size(), "unknown material ignored");
    }

    @Test
    void emptyTemplatesSectionYieldsNoTemplates() throws Exception {
        assertEquals(0, load("templates: {}\n").getTemplates().size());
    }
}
