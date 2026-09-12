package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractDiplomacyServiceTest {
    @TempDir
    Path dataDirectory;

    private ContractDatabase database;

    private static final Function<String, String> ALLIANCES = alignmentId -> {
        if (alignmentId == null) return null;
        return switch (alignmentId) {
            case "azure_hearth", "lagoon_covenant" -> "concordat_of_the_dawn";
            case "zenith_collective", "pyramid_ascendancy" -> "ironclad_syndicate";
            default -> null;
        };
    };

    @AfterEach
    void tearDown() {
        if (database != null) database.close();
    }

    private ContractDiplomacyService service(String diplomacyYml) throws Exception {
        if (diplomacyYml != null) {
            Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"), diplomacyYml);
        }
        database = new ContractDatabase(plugin());
        ContractDiplomacySettings settings = new ContractDiplomacySettings(plugin());
        settings.load();
        return new ContractDiplomacyService(database, settings, ALLIANCES);
    }

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractDiplomacyServiceTest"));
        return plugin;
    }

    private ContractDefinition def(ContractCategory category, ContractCampusRoute route) {
        return new ContractDefinition("c", category, "name", "d", 100, 30, 10, "s", "e",
                Map.of(), "obj", ContractObjectiveType.REACH_DESTINATION,
                new ContractAlignmentRule(null, null, List.of()), route, 0, true);
    }

    @Test
    void baseCompletionCreditsInfluenceToTheCompleterAlignment() throws Exception {
        ContractDiplomacyService svc = service("""
                enabled: true
                influence_per_completion: 5
                """);
        int granted = svc.recordCompletion(def(ContractCategory.COURIER, ContractCampusRoute.NONE),
                UUID.randomUUID(), "azure_hearth");
        assertEquals(5, granted);
        assertEquals(5, svc.getInfluence("azure_hearth"));
    }

    @Test
    void crossCampusDiplomaticAndEmergencyAdjustInfluence() throws Exception {
        ContractDiplomacyService svc = service("""
                influence_per_completion: 10
                cross_campus_bonus_influence: 4
                diplomatic_contract_multiplier: 2.0
                emergency_contract_influence_bonus: 3
                """);
        ContractCampusRoute cross = new ContractCampusRoute("sunway", "taylors", null, null);
        assertEquals(14, svc.recordCompletion(def(ContractCategory.COURIER, cross), null, "azure_hearth"),
                "base + cross-campus");
        assertEquals(28, svc.recordCompletion(def(ContractCategory.DIPLOMATIC, cross), null, "azure_hearth"),
                "(base + cross) * diplomatic multiplier");
        assertEquals(17, svc.recordCompletion(def(ContractCategory.EMERGENCY, cross), null, "azure_hearth"),
                "base + cross + emergency bonus");
    }

    @Test
    void differentGrandAlliancesAddCrossAllianceInfluence() throws Exception {
        ContractDiplomacyService svc = service("influence_per_completion: 10\n");
        ContractCampusRoute route = new ContractCampusRoute("taylors", "monash",
                "azure_hearth", "zenith_collective");
        int granted = svc.recordCompletion(def(ContractCategory.COURIER, route), null, "azure_hearth");
        assertTrue(granted > 10, "cross-alliance bonus added: " + granted);
        assertEquals(1, svc.getRecentInfluence(50).stream()
                .filter(r -> r.influenceType().equals("CROSS_ALLIANCE")).count());
    }

    @Test
    void influenceBetweenAlignmentsSumsBothDirections() throws Exception {
        ContractDiplomacyService svc = service("influence_per_completion: 3\n");
        ContractCampusRoute a_to_b = new ContractCampusRoute("taylors", "monash", "azure_hearth", "zenith_collective");
        ContractCampusRoute b_to_a = new ContractCampusRoute("monash", "taylors", "zenith_collective", "azure_hearth");
        svc.recordCompletion(def(ContractCategory.COURIER, a_to_b), null, "azure_hearth");
        svc.recordCompletion(def(ContractCategory.COURIER, b_to_a), null, "zenith_collective");
        // each completion also logs a CROSS_ALLIANCE row using the same pair
        assertTrue(svc.getInfluenceBetween("azure_hearth", "zenith_collective") >= 6);
    }

    @Test
    void disabledDiplomacyGrantsNothing() throws Exception {
        ContractDiplomacyService svc = service("enabled: false\ninfluence_per_completion: 9\n");
        assertEquals(0, svc.recordCompletion(def(ContractCategory.COURIER, ContractCampusRoute.NONE),
                null, "azure_hearth"));
        assertEquals(0, svc.getInfluence("azure_hearth"));
    }

    @Test
    void adminAdjustmentAppliesSignedDelta() throws Exception {
        ContractDiplomacyService svc = service("influence_per_completion: 1\n");
        svc.adminAdjustInfluence("lagoon_covenant", 25);
        svc.adminAdjustInfluence("lagoon_covenant", -5);
        assertEquals(20, svc.getInfluence("lagoon_covenant"));
    }
}
