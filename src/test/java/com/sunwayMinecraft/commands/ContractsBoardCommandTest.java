package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.contracts.service.ContractVerificationService;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import com.sunwayMinecraft.events.service.EventModifierService;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** /contracts board filters, pagination and the admin list of disabled contracts. */
class ContractsBoardCommandTest {
    private ServerMock server;
    private PlayerMock player;
    private ContractConfigManager config;
    private EventModifierService events;
    private ContractsCommands commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        player.setOp(true);
        ContractsManager manager = mock(ContractsManager.class);
        config = mock(ContractConfigManager.class);
        when(manager.getContractConfig()).thenReturn(config);
        when(manager.getAlignmentFor(player)).thenReturn("zenith_collective");
        when(manager.estimateReward(any(), any())).thenAnswer(invocation ->
                new ContractsManager.Reward(100.0, 0, false));
        commands = new ContractsCommands(manager, mock(ContractVerificationService.class));
        events = mock(EventModifierService.class);
        commands.setEventModifierService(events);

        Map<String, ContractDefinition> contracts = new LinkedHashMap<>();
        contracts.put("haul", definition("haul", ContractCategory.HAULING,
                "Lakeside Haul", new ContractAlignmentRule(null, "azure_hearth", List.of()),
                new ContractCampusRoute("taylors", "sunway", null, null)));
        contracts.put("courier", definition("courier", ContractCategory.DELIVERY,
                "Research Courier",
                new ContractAlignmentRule("zenith_collective", null, List.of()),
                new ContractCampusRoute("monash", "sunway", null, null)));
        contracts.put("restricted", definition("restricted", ContractCategory.ESCORT,
                "Azure Escort",
                new ContractAlignmentRule("azure_hearth", null, List.of()),
                new ContractCampusRoute("taylors", "taylors", null, null)));
        when(config.getContracts()).thenReturn(contracts);
        // faithful alignment-accessible filtering, reading the current board each call
        when(manager.getContractsForAlignment(any())).thenAnswer(invocation -> {
            String alignment = invocation.getArgument(0);
            return config.getContracts().values().stream()
                    .filter(def -> def.alignmentRule().canAccept(alignment)).toList();
        });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ContractDefinition definition(String id, ContractCategory category, String name,
                                          ContractAlignmentRule rule, ContractCampusRoute route) {
        return new ContractDefinition(id, category, name, "d", 100, 30, 10,
                "a", "b", Map.of(), "obj", ContractObjectiveType.REACH_DESTINATION,
                rule, route, 0, true);
    }

    private List<String> run(String... args) {
        commands.onCommand(player, command("contracts"), "contracts", args);
        return drainMessages();
    }

    private Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private List<String> drainMessages() {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) {
            // legacy serialization splits adjacent components with color codes
            messages.add(message.replaceAll("§.", ""));
        }
        return messages;
    }

    private boolean anyContains(List<String> messages, String needle) {
        return messages.stream().anyMatch(message -> message.contains(needle));
    }

    @Test
    void plainBoardShowsRouteRequiredAlignmentBoostAndPaginationHeader() {
        when(events.getPrimaryEventForCategory(ContractCategory.HAULING)).thenReturn(Optional.of(
                new CityEventDefinition("supply", CityEventType.SUPPLY_DRIVE, "Supply", "d",
                        EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60)));

        List<String> messages = run("board");
        assertTrue(anyContains(messages, "page 1/1"), "header shows page totals");
        assertTrue(anyContains(messages, "Lakeside Haul [HAULING]"), "entry shows name and type");
        assertTrue(anyContains(messages, "taylors -> sunway"), "entry shows the campus route");
        assertTrue(anyContains(messages, "requires zenith_collective"),
                "gated entry shows its required alignment");
        assertTrue(anyContains(messages, "[BOOSTED: Supply]"), "active event shows boosted status");
    }

    @Test
    void campusFilterKeepsContractsTouchingTheCampus() {
        List<String> monash = run("board", "monash");
        assertTrue(anyContains(monash, "campus monash"));
        assertTrue(anyContains(monash, "Research Courier"));
        assertTrue(!anyContains(monash, "Lakeside Haul"));

        assertTrue(anyContains(run("board", "sunway"), "Lakeside Haul"),
                "shared destination campus matches both");
        assertTrue(anyContains(run("board", "atlantis"), "No contracts match this filter."));
    }

    @Test
    void alignmentFilterResolvesRequiredAndForbiddenRules() {
        List<String> zenith = run("board", "alignment", "zenith_collective");
        assertTrue(anyContains(zenith, "Research Courier"));
        assertTrue(anyContains(zenith, "Lakeside Haul"), "open contracts match every alignment");

        List<String> azure = run("board", "alignment", "azure_hearth");
        assertTrue(anyContains(azure, "Lakeside Haul"));
        assertTrue(!anyContains(azure, "Research Courier"));

        assertTrue(anyContains(run("board", "alignment"),
                "Usage: /contracts board alignment <alignment> [page]"));
    }

    @Test
    void typeFilterMatchesCategoryCaseInsensitively() {
        assertTrue(anyContains(run("board", "type", "hauling"), "Lakeside Haul"));
        assertTrue(anyContains(run("board", "type", "DELIVERY"), "Research Courier"));

        List<String> none = run("board", "type", "sabotage");
        assertTrue(anyContains(none, "No contracts match this filter."));
    }

    @Test
    void defaultBoardHidesContractsTheViewerCannotAccept() {
        // viewer is zenith_collective, so the azure_hearth-only escort is hidden
        List<String> messages = run("board");
        assertTrue(anyContains(messages, "Research Courier"));
        assertTrue(!anyContains(messages, "Azure Escort"),
                "not-accessible contracts are hidden by default");
    }

    @Test
    void adminAllFlagRevealsEveryContractAndMarksTheHeader() {
        List<String> messages = run("board", "--all");
        assertTrue(anyContains(messages, "[ALL]"), "header notes the unrestricted view");
        assertTrue(anyContains(messages, "Azure Escort"), "--all shows not-accessible contracts");
    }

    @Test
    void campusKeywordFilterMatchesThePositionalForm() {
        List<String> monash = run("board", "campus", "monash");
        assertTrue(anyContains(monash, "campus monash"));
        assertTrue(anyContains(monash, "Research Courier"));
        assertTrue(!anyContains(monash, "Lakeside Haul"));

        assertTrue(anyContains(run("board", "campus"),
                "Usage: /contracts board campus <campus> [page]"));
    }

    @Test
    void boardShowsRecommendedAlignmentAndRewardEstimate() {
        List<String> messages = run("board", "--all");
        assertTrue(anyContains(messages, "(rec. azure_hearth)"),
                "entries surface the recommended alignment");
        assertTrue(anyContains(messages, "~$100"), "entries show a reward estimate");
    }

    @Test
    void pageArgumentSelectsTheSecondPage() {
        Map<String, ContractDefinition> many = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            String id = "c" + i;
            many.put(id, definition(id, ContractCategory.HAULING, "Haul " + i,
                    ContractAlignmentRule.OPEN, ContractCampusRoute.NONE));
        }
        when(config.getContracts()).thenReturn(many);

        List<String> second = run("board", "2");
        assertTrue(anyContains(second, "page 2/2"));
        assertTrue(anyContains(second, "Haul 9"));
        assertTrue(!anyContains(second, "Haul 0"));
    }

    @Test
    void adminListDeniesPlayersWithoutTheAdminPermission() {
        // MockBukkit players grant permissions by default, so denial is forced
        player.addAttachment(MockBukkit.createMockPlugin())
                .setPermission("sunway.contracts.admin", false);
        assertTrue(anyContains(run("admin", "list"), "do not have permission"));
    }

    @Test
    void adminListShowsDisabledContractsWithReasons() {
        Map<String, String> reasons = new LinkedHashMap<>();
        reasons.put("broken", "unknown start endpoint: nope");
        reasons.put("switched_off", "disabled in contracts.yml");
        when(config.getDisabledReasons()).thenReturn(reasons);

        List<String> shown = run("admin", "list");
        assertTrue(anyContains(shown, "Disabled Contracts"));
        assertTrue(anyContains(shown, "broken: unknown start endpoint: nope"));
        assertTrue(anyContains(shown, "switched_off: disabled in contracts.yml"));

        assertTrue(anyContains(run("admin"), "Usage: /contracts admin <list"));
    }

    @Test
    void tabCompletionCoversFiltersCampusesTypesAndAdmin() {
        List<String> second = commands.onTabComplete(player, command("contracts"), "contracts",
                new String[]{"board", ""});
        assertTrue(second.containsAll(List.of("alignment", "type", "taylors", "sunway", "monash")));

        List<String> third = commands.onTabComplete(player, command("contracts"), "contracts",
                new String[]{"board", "type", ""});
        assertTrue(third.contains("hauling"));

        List<String> admin = commands.onTabComplete(player, command("contracts"), "contracts",
                new String[]{"admin", ""});
        assertTrue(admin.contains("list"));
    }
}
