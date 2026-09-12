package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.contracts.service.ContractDiplomacyService;
import com.sunwayMinecraft.contracts.service.ContractSabotageService;
import com.sunwayMinecraft.contracts.service.ContractSupplyService;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.contracts.service.DynamicContractService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Authorization and availability behavior of the strategic command layer. */
class ContractsStrategicCommandsTest {
    private ServerMock server;
    private PlayerMock player;
    private ContractsManager manager;
    private ContractDiplomacyService diplomacy;
    private ContractSupplyService supply;
    private ContractSabotageService sabotage;
    private ContractsStrategicCommands commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        player.setOp(true); // ops satisfy the use/sabotage/admin permission checks by default
        manager = mock(ContractsManager.class);
        when(manager.getPersistence()).thenReturn(mock(ContractPersistenceService.class));
        when(manager.getAlignmentFor(any())).thenReturn("azure_hearth");
        diplomacy = mock(ContractDiplomacyService.class);
        supply = mock(ContractSupplyService.class);
        sabotage = mock(ContractSabotageService.class);
        commands = new ContractsStrategicCommands(manager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void withServices() {
        commands.setServices(diplomacy, supply, sabotage,
                mock(DynamicContractService.class), mock(ContractDiplomacySettings.class));
    }

    /** Runs a strategic subcommand and returns the player's (color-stripped) replies. */
    private List<String> run(String... args) {
        switch (args[0]) {
            case "sabotage" -> commands.sabotage((Player) player, args);
            case "influence" -> commands.influence(player, args);
            case "diplomacy" -> commands.diplomacy(player, args);
            case "supply" -> commands.supply(player, args);
            default -> throw new IllegalArgumentException(args[0]);
        }
        List<String> messages = new ArrayList<>();
        String m;
        while ((m = player.nextMessage()) != null) messages.add(m.replaceAll("§.", ""));
        return messages;
    }

    private boolean anyContains(List<String> messages, String needle) {
        return messages.stream().anyMatch(msg -> msg.contains(needle));
    }

    @Test
    void everyStrategicCommandDegradesWhenServicesAreAbsent() {
        commands.setServices(null, null, null, null, null);
        assertTrue(anyContains(run("sabotage", "5"), "not available"));
        assertTrue(anyContains(run("influence"), "not available"));
        assertTrue(anyContains(run("diplomacy"), "not available"));
        assertTrue(anyContains(run("supply"), "not available"));
    }

    @Test
    void sabotageRequiresTheSabotagePermission() {
        withServices();
        player.addAttachment(MockBukkit.createMockPlugin())
                .setPermission("sunway.contracts.sabotage", false);
        assertTrue(anyContains(run("sabotage", "5"), "permission"));
    }

    @Test
    void sabotageHappyPathRelaysServiceOutcome() {
        withServices();
        when(sabotage.attempt(eq(player), anyInt())).thenReturn(
                new ContractSabotageService.SabotageResult(true, true, "You sabotage the contract"));
        assertTrue(anyContains(run("sabotage", "5"), "sabotage the contract"));
    }

    @Test
    void influenceShowsTheViewersAlignmentTotal() {
        withServices();
        when(diplomacy.getInfluence("azure_hearth")).thenReturn(42);
        assertTrue(anyContains(run("influence"), "42"));
    }

    @Test
    void influenceForAnotherAlignmentIsAdminOnly() {
        withServices();
        player.addAttachment(MockBukkit.createMockPlugin())
                .setPermission("sunway.contracts.admin", false);
        assertTrue(anyContains(run("influence", "zenith_collective"), "permission"));
    }

    @Test
    void influenceReportsNoAlignmentWhenTheViewerIsUnaligned() {
        withServices();
        when(manager.getAlignmentFor(player)).thenReturn(null);
        assertTrue(anyContains(run("influence"), "no alignment"));
    }

    @Test
    void supplyShowsTheViewersAlignmentTotal() {
        withServices();
        when(supply.getSupplyPoints("azure_hearth")).thenReturn(11);
        assertTrue(anyContains(run("supply"), "11"));
    }

    @Test
    void diplomacyBetweenTwoAlignmentsIsAdminOnly() {
        withServices();
        player.addAttachment(MockBukkit.createMockPlugin())
                .setPermission("sunway.contracts.admin", false);
        assertFalse(anyContains(run("diplomacy", "between", "azure_hearth", "zenith_collective"),
                "Influence between"));
    }

    @Test
    void diplomacyListsRecentActivityForAdmins() {
        withServices();
        player.setOp(true);
        when(diplomacy.getRecentInfluence(anyInt())).thenReturn(List.of(
                new com.sunwayMinecraft.contracts.domain.InfluenceRecord(1, "escort",
                        null, "azure_hearth", "zenith_collective", "cd", "is", 7,
                        "COMPLETION", java.time.Instant.now())));
        assertTrue(anyContains(run("diplomacy"), "COMPLETION 7 on escort"));
    }
}
