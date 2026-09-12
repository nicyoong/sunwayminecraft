package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.config.SettingsConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.events.service.EventModifierService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ContractsManagerTest {
    private ContractConfigManager definitions;
    private SettingsConfigManager settings;
    private ContractPersistenceService persistence;
    private Economy economy;
    private Player player;
    private UUID playerId;
    private List<ActiveContract> active;
    private Map<String, Instant> cooldowns;
    private ContractsManager manager;

    @BeforeEach
    void setUp() {
        definitions = mock(ContractConfigManager.class);
        settings = mock(SettingsConfigManager.class);
        persistence = mock(ContractPersistenceService.class);
        economy = mock(Economy.class);
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        active = new ArrayList<>();
        cooldowns = new HashMap<>();
        manager = new ContractsManager(mock(JavaPlugin.class), definitions, mock(EndpointConfigManager.class), settings, persistence, economy);
        when(player.getUniqueId()).thenReturn(playerId);
        when(persistence.getPlayerContracts(playerId)).thenReturn(active);
        when(persistence.getPlayerCooldowns(playerId)).thenReturn(cooldowns);
        when(settings.getMaxActiveContracts()).thenReturn(3);
        when(definitions.getContract("contract")).thenReturn(definition("contract"));
    }

    @Test
    void acceptsKnownContractAndRejectsDuplicatesUnknownContractsLimitsAndCooldowns() {
        assertTrue(manager.acceptContract(player, "contract"));
        assertFalse(manager.acceptContract(player, "contract"));
        assertFalse(manager.acceptContract(player, "missing"));
        assertEquals(1, active.size());

        active.clear();
        when(settings.getMaxActiveContracts()).thenReturn(0);
        assertFalse(manager.acceptContract(player, "contract"));

        when(settings.getMaxActiveContracts()).thenReturn(3);
        cooldowns.put("contract", Instant.now().plusSeconds(60));
        assertFalse(manager.acceptContract(player, "contract"));
        verify(persistence, times(1)).save();
    }

    @Test
    void completionRequiresAnActiveCompletedNonExpiredContractOwnedByPlayer() {
        ActiveContract contract = activeContract("contract", playerId);
        active.add(contract);

        assertFalse(manager.completeContract(player, contract));
        contract.completeObjective();
        assertTrue(manager.completeContract(player, contract));
        verify(economy).depositPlayer(player, 50.0);
        assertTrue(active.isEmpty());
    }

    @Test
    void completionUsesEventRewardMultiplierAfterObjectiveIsCompleted() {
        ActiveContract contract = activeContract("contract", playerId);
        contract.completeObjective();
        active.add(contract);
        EventModifierService eventModifier = mock(EventModifierService.class);
        when(eventModifier.getRewardMultiplier(ContractCategory.COURIER)).thenReturn(1.5);
        manager.setEventModifierService(eventModifier);

        assertTrue(manager.completeContract(player, contract));

        verify(economy).depositPlayer(player, 75.0);
    }

    @Test
    void completionRejectsForeignOrUndefinedContractsWithoutPaying() {
        ActiveContract foreign = activeContract("contract", UUID.randomUUID());
        foreign.completeObjective();
        active.add(foreign);
        assertFalse(manager.completeContract(player, foreign));

        active.clear();
        ActiveContract missing = activeContract("missing", playerId);
        missing.completeObjective();
        active.add(missing);
        assertFalse(manager.completeContract(player, missing));
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(persistence, never()).save();
    }

    @Test
    void abandoningAppliesCooldownAndRemovesTheContract() {
        ActiveContract contract = activeContract("contract", playerId);
        active.add(contract);

        manager.abandonContract(player, contract);

        assertTrue(active.isEmpty());
        assertTrue(cooldowns.get("contract").isAfter(Instant.now()));
        verify(persistence).save();
    }

    @Test
    void cleanupExpiresContractsAppliesCooldownAndPersistsOnce() {
        ActiveContract expired = new ActiveContract(playerId, "contract", Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        ActiveContract current = activeContract("contract2", playerId);
        active.add(expired);
        active.add(current);
        when(persistence.getAllPlayerContracts()).thenReturn(Map.of(playerId, active));

        manager.cleanupExpiredContracts();

        assertEquals(List.of(current), active);
        assertTrue(cooldowns.get("contract").isAfter(Instant.now()));
        verify(persistence).save();
    }

    @Test
    void acceptanceEnforcesRequiredAndForbiddenAlignmentRules() {
        when(definitions.getContract("required")).thenReturn(definition("required",
                new ContractAlignmentRule("lagoon_covenant", null, List.of()), null, null));
        when(definitions.getContract("forbidden")).thenReturn(definition("forbidden",
                new ContractAlignmentRule(null, null, List.of("spirewrights")), null, null));

        manager.setAlignmentLookup(uuid -> Optional.of("lagoon_covenant"));
        assertTrue(manager.acceptContract(player, "required"));
        active.clear();
        // lagoon is neither required-by nor forbidden-from the second contract
        assertTrue(manager.acceptContract(player, "forbidden"));
        active.clear();

        manager.setAlignmentLookup(uuid -> Optional.of("spirewrights"));
        assertFalse(manager.acceptContract(player, "required"),
                "another alignment cannot accept a required contract");
        assertFalse(manager.acceptContract(player, "forbidden"),
                "a forbidden alignment is always refused");

        manager.setAlignmentLookup(uuid -> Optional.empty());
        assertFalse(manager.acceptContract(player, "required"),
                "unaligned players cannot accept required contracts");
    }

    @Test
    void alignmentAndCampusQueriesFilterTheEnabledContracts() {
        ContractDefinition haul = definition("haul",
                new ContractAlignmentRule(null, null, List.of("lagoon_covenant")),
                "taylors", "sunway");
        ContractDefinition gated = definition("gated",
                new ContractAlignmentRule("lagoon_covenant", null, List.of()),
                "monash", "monash");
        ContractDefinition neutral = definition("neutral", ContractAlignmentRule.OPEN,
                null, null);
        when(definitions.getContracts()).thenReturn(Map.of("haul", haul, "gated", gated,
                "neutral", neutral));

        assertEquals(List.of("gated", "neutral"),
                manager.getContractsForAlignment("lagoon_covenant").stream()
                        .map(ContractDefinition::id).sorted().toList());
        assertEquals(List.of("haul", "neutral"),
                manager.getContractsForAlignment("spirewrights").stream()
                        .map(ContractDefinition::id).sorted().toList());
        assertEquals(List.of("haul"), manager.getContractsByCampus("sunway").stream()
                .map(ContractDefinition::id).toList());
        assertEquals(List.of("haul"), manager.getContractsBetweenCampuses("taylors", "sunway").stream()
                .map(ContractDefinition::id).toList());
        assertEquals(0, manager.getContractsByCampus(null).size(),
                "a null campus matches no routes");
        assertTrue(manager.canAlignmentAcceptContract("lagoon_covenant", gated));
        assertFalse(manager.canAlignmentAcceptContract("lagoon_covenant", haul));
        assertFalse(manager.canAlignmentAcceptContract("lagoon_covenant", null));
    }

    @Test
    void endpointValidationDisablesBrokenReferencesAndMisroutedDeliveries() {
        EndpointConfigManager endpointConfig = mock(EndpointConfigManager.class);
        manager = new ContractsManager(mock(JavaPlugin.class), definitions, endpointConfig,
                settings, persistence, economy);

        ContractDefinition good = fullDefinition("good", ContractCategory.COURIER, "start", "end");
        ContractDefinition badStart = fullDefinition("bad_start", ContractCategory.COURIER,
                "missing", "end");
        ContractDefinition badDelivery = fullDefinition("bad_delivery", ContractCategory.DELIVERY,
                "start", "pickup");
        when(definitions.getContracts()).thenReturn(Map.of(
                "good", good, "bad_start", badStart, "bad_delivery", badDelivery));
        when(definitions.getContract("good")).thenReturn(good);
        when(definitions.getContract("bad_start")).thenReturn(badStart);
        when(definitions.getContract("bad_delivery")).thenReturn(badDelivery);
        when(endpointConfig.getEndpoint("start")).thenReturn(endpoint(ContractEndpoint.EndpointType.DEPOT));
        when(endpointConfig.getEndpoint("end")).thenReturn(endpoint(ContractEndpoint.EndpointType.DROPOFF));
        when(endpointConfig.getEndpoint("pickup")).thenReturn(endpoint(ContractEndpoint.EndpointType.PICKUP));

        manager.validateEndpointReferences();

        verify(definitions, never()).disableContract(eq("good"), anyString());
        verify(definitions).disableContract(eq("bad_start"), contains("unknown start endpoint"));
        verify(definitions).disableContract(eq("bad_delivery"), contains("dropoff or depot"));
    }

    private ActiveContract activeContract(String id, UUID owner) {
        return new ActiveContract(owner, id, Instant.now(), Instant.now().plusSeconds(60));
    }

    private ContractEndpoint endpoint(ContractEndpoint.EndpointType type) {
        return new ContractEndpoint("endpoint", "Endpoint", type, null, 3.0);
    }

    private ContractDefinition definition(String id) {
        return definition(id, ContractAlignmentRule.OPEN, null, null);
    }

    private ContractDefinition definition(String id, ContractAlignmentRule rule,
                                          String originCampus, String destinationCampus) {
        return new ContractDefinition(id, ContractCategory.COURIER, "name", "description", 50, 30,
                10, "start", "end", Map.of(), "objective",
                ContractObjectiveType.REACH_DESTINATION,
                rule, new ContractCampusRoute(originCampus, destinationCampus, null, null), 0, true);
    }

    private ContractDefinition fullDefinition(String id, ContractCategory category,
                                              String startEndpoint, String endEndpoint) {
        return new ContractDefinition(id, category, "name", "description", 50, 30, 10,
                startEndpoint, endEndpoint, Map.of(), "objective",
                ContractObjectiveType.REACH_DESTINATION,
                ContractAlignmentRule.OPEN, ContractCampusRoute.NONE, 0, true);
    }

    private ContractDefinition rewardDefinition(String id, double money, long rep,
                                                ContractAlignmentRule rule, ContractCampusRoute route) {
        return new ContractDefinition(id, ContractCategory.COURIER, "name", "description", money,
                30, 10, "start", "end", Map.of(), "objective",
                ContractObjectiveType.REACH_DESTINATION, rule, route, rep, true);
    }

    @Test
    void recommendedAndCrossCampusBonusesAddToMoneyAndReputation() {
        ContractDefinition def = rewardDefinition("bonus", 100, 0,
                new ContractAlignmentRule(null, "lagoon_covenant", List.of()),
                new ContractCampusRoute("sunway", "taylors", null, null));
        when(definitions.getContract("bonus")).thenReturn(def);
        when(settings.getRecommendedBonusMoney()).thenReturn(10.0);
        when(settings.getRecommendedBonusReputation()).thenReturn(2L);
        when(settings.getCrossCampusBonusMoney()).thenReturn(20.0);
        when(settings.getCrossCampusBonusReputation()).thenReturn(3L);
        manager.setAlignmentLookup(uuid -> Optional.of("lagoon_covenant"));
        java.util.List<Integer> awarded = new ArrayList<>();
        manager.setReputationRewarder((uuid, delta) -> awarded.add(delta));

        ActiveContract ac = activeContract("bonus", playerId);
        ac.completeObjective();
        active.add(ac);

        assertTrue(manager.completeContract(player, ac));
        verify(economy).depositPlayer(player, 130.0);
        assertEquals(List.of(5), awarded, "recommended (2) + cross-campus (3) reputation");
    }

    @Test
    void completionWithoutEconomyStillAwardsReputationAndRecordsStats() {
        manager = new ContractsManager(mock(JavaPlugin.class), definitions,
                mock(EndpointConfigManager.class), settings, persistence, null);
        ContractDefinition def = rewardDefinition("stats", 100, 4, ContractAlignmentRule.OPEN,
                new ContractCampusRoute("sunway", null, null, null));
        when(definitions.getContract("stats")).thenReturn(def);
        manager.setAlignmentLookup(uuid -> Optional.of("lagoon_covenant"));
        java.util.List<Integer> awarded = new ArrayList<>();
        manager.setReputationRewarder((uuid, delta) -> awarded.add(delta));

        ActiveContract ac = activeContract("stats", playerId);
        ac.completeObjective();
        active.add(ac);

        assertTrue(manager.completeContract(player, ac), "a missing Vault must not block completion");
        assertEquals(List.of(4), awarded);
        verify(persistence).recordCompletion(eq(playerId), eq("stats"), eq("lagoon_covenant"),
                eq("sunway"), any(Instant.class), eq(100.0), eq(4L));
    }

    @Test
    void unalignedPlayerCompletesOpenContractWithoutReputation() {
        when(definitions.getContract("contract")).thenReturn(
                rewardDefinition("contract", 100, 5, ContractAlignmentRule.OPEN,
                        ContractCampusRoute.NONE));
        java.util.List<Integer> awarded = new ArrayList<>();
        manager.setReputationRewarder((uuid, delta) -> awarded.add(delta));

        ActiveContract ac = activeContract("contract", playerId);
        ac.completeObjective();
        active.add(ac);

        assertTrue(manager.completeContract(player, ac));
        verify(economy).depositPlayer(player, 100.0);
        assertTrue(awarded.isEmpty(), "no alignment, so no reputation award");
        verify(persistence).recordCompletion(eq(playerId), eq("contract"), isNull(), isNull(),
                any(Instant.class), eq(100.0), eq(5L));
    }

    @Test
    void repeatedCompletionCannotDoubleAward() {
        ActiveContract ac = activeContract("contract", playerId);
        ac.completeObjective();
        active.add(ac);

        assertTrue(manager.completeContract(player, ac));
        assertFalse(manager.completeContract(player, ac), "the row is gone, so the second call no-ops");
        verify(economy, times(1)).depositPlayer(eq(player), anyDouble());
    }
}
