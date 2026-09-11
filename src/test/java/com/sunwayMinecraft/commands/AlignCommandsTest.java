package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignCommandsTest {
    private ServerMock server;
    private PlayerMock player;
    private AlignmentService service;
    private AlignmentConfigManager configManager;
    private AlignmentSettingsConfig settings;
    private AlignmentChatService chatService;
    private AlignmentMembershipCache cache;
    private AlignCommands commands;
    private AlignTabCompleter tabCompleter;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "Keepers of the flame", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        service = mock(AlignmentService.class);
        configManager = mock(AlignmentConfigManager.class);
        settings = mock(AlignmentSettingsConfig.class);
        chatService = mock(AlignmentChatService.class);
        cache = mock(AlignmentMembershipCache.class);
        when(service.isAvailable()).thenReturn(true);
        when(settings.isAllowAdminBypass()).thenReturn(true);
        when(settings.isShowJoinMessage()).thenReturn(true);
        when(settings.isShowLeaveMessage()).thenReturn(true);
        when(service.getAlignmentMemberCounts()).thenReturn(Map.of());
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(configManager.getEnabledAlignments()).thenReturn(List.of(azureHearth));
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(List.of(azureHearth));
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.IRONCLAD_SYNDICATE))
                .thenReturn(List.of());
        when(configManager.getAllianceDefinition(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(Optional.of(new GrandAllianceDefinition(
                        "concordat_of_the_dawn", "Concordat of the Dawn", "desc", "&b")));
        commands = new AlignCommands(service, configManager, settings, chatService, cache);
        tabCompleter = new AlignTabCompleter(configManager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void listShowsAllianceGroupingMemberCountsAndPageHeader() {
        when(service.getAlignmentMemberCounts()).thenReturn(Map.of("azure_hearth", 3));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"list"}));

        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("page 1/1")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("3 members")), messages.toString());
    }

    @Test
    void joinConfirmsWithAllianceCampusAndBroadcastsToOtherPlayers() {
        PlayerMock spectator = server.addPlayer("Spectator");
        when(service.join(player.getUniqueId(), "azure_hearth", false))
                .thenReturn(AlignmentResult.JOINED);

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure", "hearth"}));

        verify(service).join(player.getUniqueId(), "azure_hearth", false);
        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("taylors")), messages.toString());

        assertTrue(drainMessages(spectator).stream()
                        .anyMatch(msg -> msg.contains("has pledged to §fAzure Hearth")),
                "join broadcast must reach other online players");
    }

    @Test
    void joinShowsHumanReadableRemainingCooldown() {
        when(service.join(player.getUniqueId(), "azure_hearth", false))
                .thenReturn(AlignmentResult.COOLDOWN_ACTIVE);
        when(service.getRemainingCooldownSeconds(player.getUniqueId())).thenReturn(5400L);

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));

        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("1h 30m")),
                "cooldown message must be human readable");
    }

    @Test
    void joinRequiresAnAvailableSystemAndAnAlignmentName() {
        when(service.isAvailable()).thenReturn(false);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("unavailable")));

        when(service.isAvailable()).thenReturn(true);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"join"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Usage")));
        verify(service, never()).join(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void leaveClearsMembershipAndBroadcastsDeparture() {
        PlayerMock spectator = server.addPlayer("Spectator");
        when(service.leave(player.getUniqueId())).thenReturn(AlignmentResult.LEFT);

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"leave"}));

        verify(service).leave(player.getUniqueId());
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("left your alignment")));
        assertTrue(drainMessages(spectator).stream().anyMatch(msg -> msg.contains("is now unaligned")));
    }

    @Test
    void showReadsFromTheCacheFirst() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                player.getUniqueId(), "azure_hearth",
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                7, "active", System.currentTimeMillis())));
        when(service.getMembership(player.getUniqueId())).thenReturn(Optional.of(
                AlignmentMembership.newMembership(player.getUniqueId(), "azure_hearth", 1000L)));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));

        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Joined:")), messages.toString());
        verify(cache).getOrLoad(player.getUniqueId());
    }

    @Test
    void showReportsUnalignedPlayersAndGuardsOtherPlayerLookups() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.empty());
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unaligned")));

        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.align.admin")).thenReturn(false);
        assertTrue(commands.onCommand(nonAdmin, command("align"), "align",
                new String[]{"show", "Someone"}));
        verify(nonAdmin).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String message) -> message != null && message.contains("permission")));
    }

    @Test
    void chatDelegatesToTheAlignmentChatService() {
        when(chatService.sendAlignmentChat(player, "hello world"))
                .thenReturn(AlignmentChatService.ChatResult.SENT);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"chat", "hello", "world"}));

        when(chatService.sendAlignmentChat(player, "hi"))
                .thenReturn(AlignmentChatService.ChatResult.NO_ALIGNMENT);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"chat", "hi"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("join an alignment")));

        // /ac routes every argument as message content
        assertTrue(commands.onCommand(player, command("ac"), "ac", new String[]{"hello", "world"}));
        verify(chatService, org.mockito.Mockito.times(2))
                .sendAlignmentChat(player, "hello world");
    }

    @Test
    void chatspyRequiresItsPermissionAndToggles() {
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chatspy"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("permission")));
        verify(chatService, never()).toggleSpy(player.getUniqueId());

        player.setOp(true);
        when(chatService.toggleSpy(player.getUniqueId())).thenReturn(true);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chatspy"}));
        verify(chatService).toggleSpy(player.getUniqueId());
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("enabled")));
    }

    @Test
    void unknownSubcommandsFallBackToHelp() {
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"warp"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unknown subcommand")));
    }

    @Test
    void tabCompletionCoversSubcommandsAlignmentIdsAndPlayerNames() {
        assertEquals(List.of("help", "list", "join", "leave", "show", "chat"),
                tabCompleter.onTabComplete(player, command("align"), "align", new String[]{""}));

        assertEquals(List.of("azure_hearth"),
                tabCompleter.onTabComplete(player, command("align"), "align",
                        new String[]{"join", ""}));
        assertEquals(List.of(),
                tabCompleter.onTabComplete(player, command("align"), "align",
                        new String[]{"chat", "partial message"}));

        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("sunway.align.admin")).thenReturn(true);
        server.addPlayer("SpectatorPlayer");
        assertTrue(tabCompleter
                        .onTabComplete(admin, command("align"), "align", new String[]{"set", ""})
                        .contains("SpectatorPlayer"),
                "set must tab-complete online player names");
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
