package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignCommandsTest {
    private ServerMock server;
    private PlayerMock player;
    private AlignmentService service;
    private AlignmentConfigManager configManager;
    private AlignCommands commands;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "Keepers of the flame", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        service = mock(AlignmentService.class);
        configManager = mock(AlignmentConfigManager.class);
        when(service.isAvailable()).thenReturn(true);
        commands = new AlignCommands(service, configManager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void listGroupsEnabledAlignmentsUnderTheirGrandAlliance() {
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(List.of(azureHearth));
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.IRONCLAD_SYNDICATE))
                .thenReturn(List.of());
        when(configManager.getAllianceDefinition(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(Optional.of(new GrandAllianceDefinition(
                        "concordat_of_the_dawn", "Concordat of the Dawn", "desc", "&b")));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"list"}));

        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                "expected a grand alliance header, got: " + messages);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")),
                "expected the alignment display name, got: " + messages);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("taylors")),
                "expected the home campus, got: " + messages);
    }

    @Test
    void joinPersistsMembershipAndConfirmsWithDefinitionDetails() {
        when(service.join(player.getUniqueId(), "azure_hearth")).thenReturn(AlignmentResult.JOINED);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure", "hearth"}));

        verify(service).join(player.getUniqueId(), "azure_hearth");
        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Keepers of the flame")));
    }

    @Test
    void joinReportsUnknownDisabledAndDatabaseResults() {
        when(service.isAvailable()).thenReturn(true);
        when(service.join(player.getUniqueId(), "missing")).thenReturn(AlignmentResult.NOT_FOUND);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "missing"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unknown alignment")));

        when(service.join(player.getUniqueId(), "retired_order"))
                .thenReturn(AlignmentResult.DISABLED);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "retired_order"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("not accepting members")));

        when(service.join(player.getUniqueId(), "azure_hearth"))
                .thenReturn(AlignmentResult.DATABASE_FAILURE);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("unavailable")
                || msg.contains("Try again later")));
    }

    @Test
    void joinAndLeaveRequireTheSystemToBeAvailable() {
        when(service.isAvailable()).thenReturn(false);

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("unavailable")));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"leave"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("unavailable")));
        verify(service, org.mockito.Mockito.never()).leave(player.getUniqueId());
    }

    @Test
    void leaveReportsSuccessAndUnalignedStates() {
        when(service.leave(player.getUniqueId())).thenReturn(AlignmentResult.LEFT);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"leave"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("left your alignment")));

        when(service.leave(player.getUniqueId())).thenReturn(AlignmentResult.NOT_ALIGNED);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"leave"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("not aligned")));
    }

    @Test
    void showDisplaysOwnAlignmentForPlayersAndRequiresTargetForConsole() {
        when(service.getMembership(player.getUniqueId()))
                .thenReturn(Optional.of(new AlignmentMembership(
                        player.getUniqueId(), "azure_hearth", 1000L, 0, "active")));
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(configManager.getAllianceDefinition(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(Optional.of(new GrandAllianceDefinition(
                        "concordat_of_the_dawn", "Concordat of the Dawn", "desc", "&b")));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));
        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Joined:")));

        when(service.getMembership(player.getUniqueId())).thenReturn(Optional.empty());
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unaligned")));

        CommandSender console = mock(CommandSender.class);
        assertTrue(commands.onCommand(console, command("align"), "align", new String[]{"show"}));
        assertSentTo(console, "/align show <player>");
    }

    @Test
    void showOtherPlayerIsAdminOnly() {
        PlayerMock target = server.addPlayer("Target");
        when(service.getMembership(target.getUniqueId())).thenReturn(Optional.empty());

        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("sunway.align.admin")).thenReturn(true);
        assertTrue(commands.onCommand(admin, command("align"), "align",
                new String[]{"show", "Target"}));
        assertSentTo(admin, "Unaligned");

        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.align.admin")).thenReturn(false);
        assertTrue(commands.onCommand(nonAdmin, command("align"), "align",
                new String[]{"show", "Target"}));
        assertSentTo(nonAdmin, "permission");
    }

    @Test
    void tabCompletionSuggestsSubcommandsAlignmentIdsAndPlayerNames() {
        when(configManager.getEnabledAlignments()).thenReturn(List.of(azureHearth));

        assertEquals(List.of("help", "list", "join", "leave", "show"),
                commands.onTabComplete(player, command("align"), "align", new String[]{""}));
        assertEquals(List.of("azure_hearth"),
                commands.onTabComplete(player, command("align"), "align", new String[]{"join", ""}));
        assertEquals(List.of(),
                commands.onTabComplete(player, command("align"), "align", new String[]{"show", ""}));
    }

    private void assertSentTo(CommandSender sender, String needle) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(message -> message.contains(needle)),
                "expected a message containing '" + needle + "', got: " + captor.getAllValues());
    }

    private List<String> drainMessages(PlayerMock recipient) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = recipient.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }

    private Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }
}
