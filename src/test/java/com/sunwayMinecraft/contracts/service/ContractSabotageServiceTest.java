package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import org.bukkit.entity.Player;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractSabotageServiceTest {
    @TempDir
    Path dataDirectory;

    private ServerMock server;
    private ContractPersistenceService persistence;
    private ContractDiplomacyService diplomacy;
    private final List<Integer> reputationDeltas = new ArrayList<>();
    private UUID victimId;
    // default: the victim is pyramid_ascendancy, everyone else azure_hearth (hostile pair)
    private final Map<UUID, String> alignments = new HashMap<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        victimId = UUID.randomUUID();
        alignments.put(victimId, "pyramid_ascendancy");
    }

    @AfterEach
    void tearDown() {
        if (persistence != null) persistence.close();
        MockBukkit.unmock();
    }

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContractSabotageServiceTest"));
        return plugin;
    }

    private Player playerWith(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        return player;
    }

    private ContractSabotageService service(String diplomacyYml, java.util.Random random) throws Exception {
        Files.writeString(dataDirectory.resolve("contract-diplomacy.yml"), diplomacyYml);
        persistence = new ContractPersistenceService(plugin());
        ContractDatabase db = persistence.getDatabase();
        ContractDiplomacySettings settings = new ContractDiplomacySettings(plugin());
        settings.load();
        ContractConfigManager config = mock(ContractConfigManager.class);
        when(config.getContract("stone")).thenReturn(new ContractDefinition("stone",
                ContractCategory.HAULING, "Stone", "d", 100, 30, 10, "s", "e", Map.of(), "obj",
                ContractObjectiveType.DELIVER_MATERIALS,
                new ContractAlignmentRule(null, null, List.of()), ContractCampusRoute.NONE, 0, true));
        diplomacy = new ContractDiplomacyService(db, settings, id -> null);
        Function<UUID, Optional<String>> lookup =
                uuid -> Optional.ofNullable(alignments.getOrDefault(uuid, "azure_hearth"));
        return new ContractSabotageService(db, persistence, config, settings, diplomacy, null,
                lookup, (uuid, delta) -> reputationDeltas.add(delta), plugin(), random);
    }

    /** Persists a victim active contract and returns its database row id. */
    private int victimActiveContract(String contractId, boolean complete) {
        ActiveContract ac = new ActiveContract(victimId, contractId, Instant.now(),
                Instant.now().plusSeconds(600));
        if (complete) ac.completeObjective();
        persistence.getPlayerContracts(victimId).add(ac);
        persistence.save();
        return persistence.getDatabase().getActiveContracts().stream()
                .filter(c -> c.getPlayerUuid().equals(victimId) && c.getContractId().equals(contractId))
                .map(ActiveContract::getActiveId).findFirst().orElseThrow();
    }

    @Test
    void successfulSabotageResetsVictimProgressAndMarksSabotaged() throws Exception {
        ContractSabotageService svc = service("sabotage_enabled: true\nsabotage_success_chance: 1.0\n",
                new java.util.Random(0));
        int id = victimActiveContract("stone", false);

        ContractSabotageService.SabotageResult result = svc.attempt(playerWith("Griever"), id);
        assertTrue(result.attempted());
        assertTrue(result.success());
        ActiveContract victimAc = persistence.getPlayerContracts(victimId).get(0);
        assertFalse(victimAc.isObjectiveComplete());
        assertTrue(victimAc.hasStage(ContractSabotageService.STAGE_SABOTAGED));
    }

    @Test
    void failedSabotageChargesReputationPenalty() throws Exception {
        ContractSabotageService svc = service(
                "sabotage_enabled: true\nsabotage_success_chance: 0.0\nsabotage_failure_penalty: 2\n",
                new java.util.Random(0));
        int id = victimActiveContract("stone", false);

        ContractSabotageService.SabotageResult result = svc.attempt(playerWith("Clumsy"), id);
        assertTrue(result.attempted());
        assertFalse(result.success());
        assertEquals(List.of(-2), reputationDeltas);
    }

    @Test
    void cooldownBlocksARapidSecondAttempt() throws Exception {
        ContractSabotageService svc = service(
                "sabotage_enabled: true\nsabotage_success_chance: 0.0\nsabotage_cooldown_seconds: 600\n",
                new java.util.Random(0));
        Player saboteur = playerWith("Rusher");
        int id = victimActiveContract("stone", false);
        assertTrue(svc.attempt(saboteur, id).attempted());
        ContractSabotageService.SabotageResult second = svc.attempt(saboteur, id);
        assertFalse(second.attempted());
        assertTrue(second.message().contains("cooldown"));
    }

    @Test
    void selfSabotageIsRefused() throws Exception {
        ContractSabotageService svc = service("sabotage_enabled: true\nsabotage_success_chance: 1.0\n",
                new java.util.Random(0));
        int id = victimActiveContract("stone", false);
        Player self = mock(Player.class);
        when(self.getUniqueId()).thenReturn(victimId);
        ContractSabotageService.SabotageResult result = svc.attempt(self, id);
        assertFalse(result.attempted());
        assertTrue(result.message().contains("own"));
    }

    @Test
    void friendlySabotageIsRefused() throws Exception {
        ContractSabotageService svc = service("sabotage_enabled: true\nsabotage_success_chance: 1.0\n",
                new java.util.Random(0));
        Player ally = playerWith("Ally");
        alignments.put(ally.getUniqueId(), "pyramid_ascendancy"); // same as the victim
        int id = victimActiveContract("stone", false);
        ContractSabotageService.SabotageResult result = svc.attempt(ally, id);
        assertFalse(result.attempted());
        assertTrue(result.message().contains("ally"));
    }

    @Test
    void disabledSabotageRefusesUnlessAdminBypass() throws Exception {
        ContractSabotageService svc = service("sabotage_enabled: false\n", new java.util.Random(0));
        int id = victimActiveContract("stone", false);
        assertFalse(svc.attempt(playerWith("X"), id).attempted());
        assertTrue(svc.attempt(playerWith("X"), id, true).success()); // admin bypass forces success
    }

    @Test
    void completedContractCannotBeSabotaged() throws Exception {
        ContractSabotageService svc = service("sabotage_enabled: true\nsabotage_success_chance: 1.0\n",
                new java.util.Random(0));
        int id = victimActiveContract("stone", true);
        ContractSabotageService.SabotageResult result = svc.attempt(playerWith("TooLate"), id);
        assertFalse(result.attempted());
        assertTrue(result.message().contains("complete"));
    }
}
