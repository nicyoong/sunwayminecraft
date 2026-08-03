package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.city.CityOverviewService;
import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.contracts.service.ContractVerificationService;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.events.config.EventConfigManager;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import com.sunwayMinecraft.events.service.CityEventsManager;
import com.sunwayMinecraft.worldtravel.MiningWorldEvacuationManager;
import com.sunwayMinecraft.worldtravel.MiningWorldState;
import com.sunwayMinecraft.worldtravel.WorldTravelManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreCommandBehaviorTest {
    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void worldTravelCommandsRejectConsoleTravelDelegatePlayerTravelAndExposeInfoToAnySender() {
        WorldTravelManager manager = mock(WorldTravelManager.class);
        when(manager.buildMiningInfoMessage()).thenReturn(Component.text("info"));
        WorldTravelCommands commands = new WorldTravelCommands(manager);
        CommandSender console = mock(CommandSender.class);

        assertTrue(commands.onCommand(console, command("mineworld"), "mineworld", new String[0]));
        verify(manager, never()).teleportToMining(any());

        when(manager.isMiningWorld(player)).thenReturn(false);
        when(manager.teleportToMining(player)).thenReturn(true);
        assertTrue(commands.onCommand(player, command("mineworld"), "mineworld", new String[0]));
        verify(manager).teleportToMining(player);

        assertTrue(commands.onCommand(console, command("mininginfo"), "mininginfo", new String[0]));
        verify(console).sendMessage(Component.text("info"));
    }

    @Test
    void miningAdminCommandValidatesEvacuationArgumentsAndChangesStateOnlyWhenAllowed() {
        WorldTravelManager travel = mock(WorldTravelManager.class);
        MiningWorldEvacuationManager evacuation = mock(MiningWorldEvacuationManager.class);
        MiningWorldAdminCommands commands = new MiningWorldAdminCommands(travel, evacuation);
        CommandSender sender = mock(CommandSender.class);

        assertTrue(commands.onCommand(sender, command("miningevacuate"), "miningevacuate", new String[]{"bad"}));
        verify(evacuation, never()).startEvacuation(org.mockito.ArgumentMatchers.anyInt());

        when(evacuation.startEvacuation(10)).thenReturn(true);
        assertTrue(commands.onCommand(sender, command("miningevacuate"), "miningevacuate", new String[]{"10"}));
        verify(evacuation).startEvacuation(10);

        when(evacuation.isEvacuationRunning()).thenReturn(false);
        assertTrue(commands.onCommand(sender, command("mininglock"), "mininglock", new String[0]));
        verify(travel).setMiningWorldState(MiningWorldState.LOCKED);
    }

    @Test
    void contractsCommandDelegatesAcceptListsTabCompletionAndRejectsConsoleUse() {
        ContractsManager manager = mock(ContractsManager.class);
        ContractConfigManager config = mock(ContractConfigManager.class);
        ContractPersistenceService persistence = mock(ContractPersistenceService.class);
        when(manager.getContractConfig()).thenReturn(config);
        when(manager.getPersistence()).thenReturn(persistence);
        when(config.getContracts()).thenReturn(Map.of("haul", new ContractDefinition("haul", ContractCategory.HAULING,
                "Haul", "", 1, 1, 1, "a", "b", Map.of(), "go")));
        when(persistence.getPlayerContracts(player.getUniqueId())).thenReturn(new ArrayList<>());
        when(manager.acceptContract(player, "haul")).thenReturn(true);
        ContractsCommands commands = new ContractsCommands(manager, mock(ContractVerificationService.class));

        assertTrue(commands.onCommand(player, command("contracts"), "contracts", new String[]{"accept", "haul"}));
        verify(manager).acceptContract(player, "haul");
        assertEquals(List.of("haul"), commands.onTabComplete(player, command("contracts"), "contracts", new String[]{"accept", ""}));

        CommandSender console = mock(CommandSender.class);
        assertTrue(commands.onCommand(console, command("contracts"), "contracts", new String[]{"board"}));
    }

    @Test
    void eventsAndCityCommandsHandleHelpAndTabCompletionWithoutRequiringLiveManagers() {
        CityEventsManager events = mock(CityEventsManager.class);
        EventConfigManager eventConfig = mock(EventConfigManager.class);
        when(events.getConfigManager()).thenReturn(eventConfig);
        CityEventDefinition event = new CityEventDefinition("supply", CityEventType.SUPPLY_DRIVE, "Supply", "",
                EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60);
        when(eventConfig.getEvents()).thenReturn(Map.of("supply", event));
        when(eventConfig.getEvent("supply")).thenReturn(event);
        EventsCommands eventCommands = new EventsCommands(events);

        assertEquals(List.of("current", "upcoming", "info", "help"),
                eventCommands.onTabComplete(player, command("events"), "events", new String[]{""}));
        assertEquals(List.of("supply"), eventCommands.onTabComplete(player, command("events"), "events", new String[]{"info", ""}));
        assertTrue(eventCommands.onCommand(player, command("events"), "events", new String[]{"info", "missing"}));

        CityCommands cityCommands = new CityCommands(mock(CityOverviewService.class));
        CommandSender console = mock(CommandSender.class);
        assertTrue(cityCommands.onCommand(console, command("city"), "city", new String[0]));
        assertEquals(List.of("status", "guide", "help"), cityCommands.onTabComplete(player, command("city"), "city", new String[]{""}));
    }

    private Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }
}
